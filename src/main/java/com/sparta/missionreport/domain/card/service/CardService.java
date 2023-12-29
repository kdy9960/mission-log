package com.sparta.missionreport.domain.card.service;

import com.sparta.missionreport.domain.board.entity.Board;
import com.sparta.missionreport.domain.board.service.BoardService;
import com.sparta.missionreport.domain.card.dto.CardDto;
import com.sparta.missionreport.domain.card.dto.CardWorkerDto;
import com.sparta.missionreport.domain.card.entity.Card;
import com.sparta.missionreport.domain.card.exception.CardCustomException;
import com.sparta.missionreport.domain.card.exception.CardExceptionCode;
import com.sparta.missionreport.domain.card.repository.CardRepository;
import com.sparta.missionreport.domain.column.entity.Columns;
import com.sparta.missionreport.domain.column.service.ColumnsService;
import com.sparta.missionreport.domain.user.entity.User;
import com.sparta.missionreport.domain.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final UserService userService;
    private final BoardService boardService;
    private final ColumnsService columnsService;
    private final CardWorkerService cardWorkerService;
    private final CardRepository cardRepository;

    @Transactional
    public CardDto.CardResponse createCard(User user, Long columnId,
                                           CardDto.CreateCardRequest createCardRequest) {
        Columns columns = columnsService.findColumns(columnId);
        User createdBy = userService.findUserById(user.getId());

        /* TODO: createdBy 가 Board 에 권한을 가진 사람인지 확인하는 코드*/

        long sequence = cardRepository.countByColumns_IdAndIsDeletedIsFalse(columnId) + 1;
        Card savedCard = cardRepository.save(createCardRequest.toEntity(sequence, columns, createdBy));

        cardWorkerService.saveCardWorker(savedCard, createdBy);

        return CardDto.CardResponse.of(savedCard);
    }


    @Transactional
    public CardDto.CardResponse update(User user, Long cardId, CardDto.UpdateCardRequest updateCardRequest) {
        Card card = getCardAndCheckAuth(user, cardId);

        card.update(updateCardRequest);
        return CardDto.CardResponse.of(card);
    }

    @Transactional
    public void deleteCard(User user, Long cardId) {
        Card card = getCardAndCheckAuth(user, cardId);
        card.deleteCard();
        cardWorkerService.deleteWorkers(card);

        long sequence = card.getSequence();
        long last = cardRepository.findTopByColumns_IdAndIsDeletedIsFalseOrderBySequenceDesc(
                        card.getColumns().getId())
                .orElseThrow(() -> new CardCustomException(CardExceptionCode.CARD_NOT_FOUND))
                .getSequence();

        cardRepository.decreaseSequence(card.getColumns().getId(), sequence, last);
    }

    public List<CardDto.CardResponse> getCardsByBoard(User user, Long boardId) {
        Board board = boardService.findBoardByID(boardId);
        /* TODO: 로그인 유저가 해당 보드 작업자 여부 확인 코드 작성 */

        List<Card> cards = cardRepository.findAllByColumns_Board_IdAndIsDeletedIsFalse(boardId);
        return cards.stream().map(CardDto.CardResponse::of).toList();
    }

    public CardDto.CardResponse getCard(User user, Long cardId) {
        Card card = getCardAndCheckAuth(user, cardId);
        return CardDto.CardResponse.of(card);
    }

    @Transactional
    public void inviteUser(User user, Long cardId, CardWorkerDto.CardWorkerInviteRequest requestDto) {
        Card card = getCardAndCheckAuth(user, cardId);
        User requestUser = userService.findUserByEmail(requestDto.getEmail());

        if (cardWorkerService.isExistedWorker(requestUser, card)) {
            throw new CardCustomException(CardExceptionCode.ALREADY_INVITED_USER);
        }

        /* TODO: 해당 요청 유저가 보드에 권한을 가지고 있는지 체크하는 코드 */
        cardWorkerService.saveCardWorker(card, requestUser);
    }

    public Card getCardAndCheckAuth(User user, Long cardId) {
        Card card = findCardById(cardId);
        User loginUser = userService.findUserById(user.getId());

        if (!cardWorkerService.isExistedWorker(loginUser, card)) {
            throw new CardCustomException(CardExceptionCode.NOT_AUTHORIZATION_ABOUT_CARD);
        }

        return card;
    }


    public Card findCardById(Long cardId) {
        return cardRepository.findByIdAndIsDeletedIsFalse(cardId).orElseThrow(
                () -> new CardCustomException(CardExceptionCode.CARD_NOT_FOUND)
        );
    }

    public void getWorkersInCard(User user, Long cardId) {

    }
}
