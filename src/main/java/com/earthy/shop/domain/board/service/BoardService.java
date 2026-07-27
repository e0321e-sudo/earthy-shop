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
import com.earthy.shop.domain.board.enums.BoardVisibility;
import com.earthy.shop.domain.board.repository.BoardRepository;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    // 게시글 작성
    @Transactional
    public BoardResponseDto createBoard(String email, BoardCreateRequestDto requestDto) {
        // 작성 회원 조회
        Member member = memberService.getActiveMember(email);

        // 비공개 게시글 비밀번호 검증
        validatePrivatePassword(requestDto.getVisibility(), requestDto.getPostPassword());

        // 비공개 게시글 비밀번호 암호화
        String encodedPostPassword = encodePostPassword(
                requestDto.getVisibility(),
                requestDto.getPostPassword()
        );

        Board board = new Board(
                member,
                requestDto.getType(),
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getVisibility(),
                encodedPostPassword
        );

        Board savedBoard = boardRepository.save(board);

        return BoardResponseDto.of(savedBoard, true);
    }

    // 게시글 목록 조회
    public PageResponseDto<BoardListResponseDto> getBoards(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return PageResponseDto.from(
                    boardRepository.findAllByOrderByCreatedAtDesc(pageable)
                            .map(BoardListResponseDto::from)
            );
        }

        return PageResponseDto.from(
                boardRepository.searchByKeyword(keyword, pageable)
                        .map(BoardListResponseDto::from)
        );
    }

    // 공개 게시글 상세 조회
    public BoardResponseDto getPublicBoard(String email, Long boardId) {
        Board board = getBoard(boardId);

        if (board.isPrivate()) {
            throw new BusinessException(ErrorCode.BOARD_PASSWORD_REQUIRED);
        }

        return BoardResponseDto.of(board, isMine(email, board));
    }

    // 비공개 게시글 상세 조회
    public BoardResponseDto getPrivateBoard(String email, Long boardId, BoardPrivateCheckRequestDto requestDto) {
        Board board = getBoard(boardId);

        if (!board.isPrivate()) {
            return BoardResponseDto.of(board, isMine(email, board));
        }

        if (!passwordEncoder.matches(requestDto.getPostPassword(), board.getPostPassword())) {
            throw new BusinessException(ErrorCode.BOARD_PASSWORD_MISMATCH);
        }

        return BoardResponseDto.of(board, isMine(email, board));
    }

    // 게시글 수정
    @Transactional
    public BoardResponseDto updateBoard(String email, Long boardId, BoardUpdateRequestDto requestDto) {
        // 작성 회원 조회
        Member member = memberService.getActiveMember(email);

        // 게시글 조회
        Board board = getBoard(boardId);

        // 작성자 본인 여부 검증
        validateBoardWriter(board, member);

        // 답변 등록 여부 검증
        if (!board.canUpdate()) {
            throw new BusinessException(ErrorCode.BOARD_ALREADY_ANSWERED);
        }

        // 수정 후 공개 여부에 따른 비밀번호 결정
        String resolvedPostPassword = resolvePostPassword(
                board,
                requestDto.getVisibility(),
                requestDto.getPostPassword()
        );

        board.update(
                requestDto.getType(),
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getVisibility(),
                resolvedPostPassword
        );

        return BoardResponseDto.of(board, true);
    }

    // 관리자 게시글 목록 조회
    public PageResponseDto<AdminBoardResponseDto> getAdminBoards(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return PageResponseDto.from(
                    boardRepository.findAllByOrderByCreatedAtDesc(pageable)
                            .map(AdminBoardResponseDto::from)
            );
        }

        return PageResponseDto.from(
                boardRepository.searchByKeyword(keyword, pageable)
                        .map(AdminBoardResponseDto::from)
        );
    }

    // 관리자 게시글 상세 조회
    public AdminBoardResponseDto getAdminBoard(Long boardId) {
        Board board = getBoard(boardId);

        return AdminBoardResponseDto.from(board);
    }

    // 관리자 답변 등록
    @Transactional
    public AdminBoardResponseDto answerBoard(Long boardId, BoardAnswerRequestDto requestDto) {
        Board board = getBoard(boardId);

        board.answer(requestDto.getAnswer());

        return AdminBoardResponseDto.from(board);
    }

    // 게시글 엔티티 조회
    public Board getBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
    }

    // 작성자 본인 여부 검증
    private void validateBoardWriter(Board board, Member member) {
        if (!board.isWrittenBy(member)) {
            throw new BusinessException(ErrorCode.BOARD_ACCESS_DENIED);
        }
    }

    // 현재 로그인 회원의 게시글 여부
    private boolean isMine(String email, Board board) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Member member = memberService.getActiveMember(email);

        return board.isWrittenBy(member);
    }

    // 비공개 게시글 비밀번호 검증
    private void validatePrivatePassword(BoardVisibility visibility, String postPassword) {
        if (visibility == BoardVisibility.PRIVATE && (postPassword == null || postPassword.isBlank())) {
            throw new BusinessException(ErrorCode.BOARD_PASSWORD_REQUIRED);
        }
    }

    // 비공개 게시글 비밀번호 암호화
    private String encodePostPassword(BoardVisibility visibility, String postPassword) {
        if (visibility == BoardVisibility.PUBLIC) {
            return null;
        }

        return passwordEncoder.encode(postPassword);
    }

    // 수정 후 공개 여부에 따른 게시글 비밀번호 결정
    private String resolvePostPassword(Board board, BoardVisibility visibility, String postPassword) {
        if (visibility == BoardVisibility.PUBLIC) {
            return null;
        }

        // 비밀번호 미입력 시 기존 비밀번호 유지
        if (postPassword == null || postPassword.isBlank()) {
            if (board.getPostPassword() == null) {
                throw new BusinessException(ErrorCode.BOARD_PASSWORD_REQUIRED);
            }

            return board.getPostPassword();
        }

        // 새 비밀번호 입력 시 길이 검증
        if (postPassword.length() < 4 || postPassword.length() > 20) {
            throw new BusinessException(ErrorCode.INVALID_BOARD_PASSWORD);
        }

        return passwordEncoder.encode(postPassword);
    }
}
