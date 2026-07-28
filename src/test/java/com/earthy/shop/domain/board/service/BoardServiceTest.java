package com.earthy.shop.domain.board.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.board.dto.request.BoardAnswerRequestDto;
import com.earthy.shop.domain.board.dto.request.BoardCreateRequestDto;
import com.earthy.shop.domain.board.dto.request.BoardPrivateCheckRequestDto;
import com.earthy.shop.domain.board.dto.request.BoardUpdateRequestDto;
import com.earthy.shop.domain.board.dto.response.AdminBoardResponseDto;
import com.earthy.shop.domain.board.dto.response.BoardListResponseDto;
import com.earthy.shop.domain.board.dto.response.BoardResponseDto;
import com.earthy.shop.domain.board.entity.Board;
import com.earthy.shop.domain.board.enums.BoardStatus;
import com.earthy.shop.domain.board.enums.BoardType;
import com.earthy.shop.domain.board.enums.BoardVisibility;
import com.earthy.shop.domain.board.repository.BoardRepository;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BoardService boardService;

    @Test
    void 게시글_목록을_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Board board = board(member(1L, "홍길동"), BoardVisibility.PUBLIC, null);

        given(boardRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(board), pageable, 1));

        // when
        PageResponseDto<BoardListResponseDto> response = boardService.getBoards(null, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().title()).isEqualTo("배송 문의");
    }

    @Test
    void 게시글을_검색한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Board board = board(member(1L, "홍길동"), BoardVisibility.PUBLIC, null);

        given(boardRepository.searchByKeyword("배송", pageable))
                .willReturn(new PageImpl<>(List.of(board), pageable, 1));

        // when
        PageResponseDto<BoardListResponseDto> response = boardService.getBoards("배송", pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().title()).contains("배송");
    }

    @Test
    void 공개_게시글을_작성한다() {
        // given
        Member member = member(1L, "홍길동");
        BoardCreateRequestDto requestDto = new BoardCreateRequestDto(
                BoardType.PRODUCT,
                "배송 문의",
                "언제 배송되나요?",
                BoardVisibility.PUBLIC,
                null
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(boardRepository.save(any(Board.class)))
                .willAnswer(invocation -> {
                    Board board = invocation.getArgument(0);
                    TestEntityUtils.setId(board, 1L);
                    return board;
                });

        // when
        BoardResponseDto response = boardService.createBoard("test@example.com", requestDto);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("배송 문의");
        assertThat(response.visibility()).isEqualTo(BoardVisibility.PUBLIC);
        assertThat(response.mine()).isTrue();
    }

    @Test
    void 비공개_게시글_작성_시_비밀번호가_없으면_예외가_발생한다() {
        // given
        Member member = member(1L, "홍길동");
        BoardCreateRequestDto requestDto = new BoardCreateRequestDto(
                BoardType.PRODUCT,
                "비밀 문의",
                "교환 문의입니다.",
                BoardVisibility.PRIVATE,
                ""
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.createBoard("test@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_PASSWORD_REQUIRED)
                );
    }

    @Test
    void 공개_게시글을_상세_조회한다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PUBLIC, null);

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(memberService.getActiveMember("test@example.com")).willReturn(member);

        // when
        BoardResponseDto response = boardService.getPublicBoard("test@example.com", 1L);

        // then
        assertThat(response.title()).isEqualTo("배송 문의");
        assertThat(response.writerName()).isEqualTo("홍**");
        assertThat(response.mine()).isTrue();
    }

    @Test
    void 비로그인_공개글_조회_시_내글이_아니다() {
        // given
        Board board = board(member(1L, "홍길동"), BoardVisibility.PUBLIC, null);

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        BoardResponseDto response = boardService.getPublicBoard(null, 1L);

        // then
        assertThat(response.mine()).isFalse();
    }

    @Test
    void 로그인했지만_작성자가_아니면_내글이_아니다() {
        // given
        Member writer = member(1L, "홍길동");
        Member viewer = member(2L, "이순신");
        Board board = board(writer, BoardVisibility.PUBLIC, null);

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(memberService.getActiveMember("viewer@example.com")).willReturn(viewer);

        // when
        BoardResponseDto response = boardService.getPublicBoard("viewer@example.com", 1L);

        // then
        assertThat(response.mine()).isFalse();
    }

    @Test
    void 비공개_게시글을_공개_조회하면_예외가_발생한다() {
        // given
        Board board = board(member(1L, "홍길동"), BoardVisibility.PRIVATE, "encodedPassword");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.getPublicBoard("test@example.com", 1L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_PASSWORD_REQUIRED)
                );
    }

    @Test
    void 비공개_게시글_비밀번호가_일치하면_상세_조회한다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PRIVATE, "encodedPassword");
        BoardPrivateCheckRequestDto requestDto = new BoardPrivateCheckRequestDto("1234");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(passwordEncoder.matches("1234", "encodedPassword")).willReturn(true);
        given(memberService.getActiveMember("test@example.com")).willReturn(member);

        // when
        BoardResponseDto response = boardService.getPrivateBoard("test@example.com", 1L, requestDto);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.mine()).isTrue();
    }

    @Test
    void 비공개_게시글_비밀번호가_틀리면_예외가_발생한다() {
        // given
        Board board = board(member(1L, "홍길동"), BoardVisibility.PRIVATE, "encodedPassword");
        BoardPrivateCheckRequestDto requestDto = new BoardPrivateCheckRequestDto("9999");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(passwordEncoder.matches("9999", "encodedPassword")).willReturn(false);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.getPrivateBoard("test@example.com", 1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_PASSWORD_MISMATCH)
                );
    }

    @Test
    void 작성자라면_답변_전_게시글을_수정한다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PUBLIC, null);
        BoardUpdateRequestDto requestDto = new BoardUpdateRequestDto(
                BoardType.DELIVERY,
                "배송 문의 수정",
                "수정된 내용",
                BoardVisibility.PRIVATE,
                "1234"
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(passwordEncoder.encode("1234")).willReturn("encodedNewPassword");

        // when
        BoardResponseDto response = boardService.updateBoard("test@example.com", 1L, requestDto);

        // then
        assertThat(response.title()).isEqualTo("배송 문의 수정");
        assertThat(response.visibility()).isEqualTo(BoardVisibility.PRIVATE);
        assertThat(board.getPostPassword()).isEqualTo("encodedNewPassword");
    }

    @Test
    void 작성자가_아니면_게시글을_수정할_수_없다() {
        // given
        Member writer = member(1L, "홍길동");
        Member otherMember = member(2L, "이순신");
        Board board = board(writer, BoardVisibility.PUBLIC, null);
        BoardUpdateRequestDto requestDto = new BoardUpdateRequestDto(
                BoardType.PRODUCT,
                "수정 제목",
                "수정 내용",
                BoardVisibility.PUBLIC,
                null
        );

        given(memberService.getActiveMember("other@example.com")).willReturn(otherMember);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.updateBoard("other@example.com", 1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_ACCESS_DENIED)
                );
    }

    @Test
    void 비공개_게시글_수정_시_비밀번호를_비워두면_기존_비밀번호를_유지한다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PRIVATE, "encodedPassword");
        BoardUpdateRequestDto requestDto = new BoardUpdateRequestDto(
                BoardType.PRODUCT,
                "수정 제목",
                "수정 내용",
                BoardVisibility.PRIVATE,
                ""
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        BoardResponseDto response = boardService.updateBoard("test@example.com", 1L, requestDto);

        // then
        assertThat(response.visibility()).isEqualTo(BoardVisibility.PRIVATE);
        assertThat(board.getPostPassword()).isEqualTo("encodedPassword");
    }

    @Test
    void 비공개_게시글_수정_시_새_비밀번호_길이가_짧으면_예외가_발생한다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PRIVATE, "encodedPassword");
        BoardUpdateRequestDto requestDto = new BoardUpdateRequestDto(
                BoardType.PRODUCT,
                "수정 제목",
                "수정 내용",
                BoardVisibility.PRIVATE,
                "123"
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.updateBoard("test@example.com", 1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BOARD_PASSWORD)
                );
    }

    @Test
    void 비공개_게시글을_공개로_수정하면_비밀번호를_제거한다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PRIVATE, "encodedPassword");
        BoardUpdateRequestDto requestDto = new BoardUpdateRequestDto(
                BoardType.PRODUCT,
                "공개 전환",
                "공개 내용",
                BoardVisibility.PUBLIC,
                null
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        BoardResponseDto response = boardService.updateBoard("test@example.com", 1L, requestDto);

        // then
        assertThat(response.visibility()).isEqualTo(BoardVisibility.PUBLIC);
        assertThat(board.getPostPassword()).isNull();
    }

    @Test
    void 공개_게시글을_비공개로_수정할_때_비밀번호가_없으면_예외가_발생한다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PUBLIC, null);
        BoardUpdateRequestDto requestDto = new BoardUpdateRequestDto(
                BoardType.PRODUCT,
                "비공개 전환",
                "비공개 내용",
                BoardVisibility.PRIVATE,
                null
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.updateBoard("test@example.com", 1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_PASSWORD_REQUIRED)
                );
    }

    @Test
    void 게시글이_없으면_예외가_발생한다() {
        // given
        given(boardRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.getBoard(1L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_NOT_FOUND)
                );
    }

    @Test
    void 답변된_게시글은_수정할_수_없다() {
        // given
        Member member = member(1L, "홍길동");
        Board board = board(member, BoardVisibility.PUBLIC, null);
        board.answer("답변입니다.");
        BoardUpdateRequestDto requestDto = new BoardUpdateRequestDto(
                BoardType.PRODUCT,
                "수정 제목",
                "수정 내용",
                BoardVisibility.PUBLIC,
                null
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> boardService.updateBoard("test@example.com", 1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOARD_ALREADY_ANSWERED)
                );
    }

    @Test
    void 관리자가_답변을_등록한다() {
        // given
        Board board = board(member(1L, "홍길동"), BoardVisibility.PUBLIC, null);
        BoardAnswerRequestDto requestDto = new BoardAnswerRequestDto("답변입니다.");

        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        AdminBoardResponseDto response = boardService.answerBoard(1L, requestDto);

        // then
        assertThat(response.answer()).isEqualTo("답변입니다.");
        assertThat(response.status()).isEqualTo(BoardStatus.ANSWERED);
        assertThat(board.getAnsweredAt()).isNotNull();
    }

    private Member member(Long id, String name) {
        Member member = new Member("test@example.com", "encodedPassword", name, "010-1234-5678");
        TestEntityUtils.setId(member, id);
        return member;
    }

    private Board board(Member member, BoardVisibility visibility, String postPassword) {
        Board board = new Board(
                member,
                BoardType.PRODUCT,
                "배송 문의",
                "언제 배송되나요?",
                visibility,
                postPassword
        );
        TestEntityUtils.setId(board, 1L);
        return board;
    }
}
