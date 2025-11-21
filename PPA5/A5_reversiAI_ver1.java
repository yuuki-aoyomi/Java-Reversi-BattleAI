package j2.review02;

import java.util.ArrayList;
import java.util.Random;
// TODO: 使用者の環境に応じてパッケージ名を変更してください
import j2.review02.s24kXXXX.AI; // 使用者の環境に応じて変更してください

public class A5_reversiAI_ver1 extends AI {

    private final int depthLimit;
    private final Random random;
    private Location result;

    public A5_reversiAI_ver1(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        this.depthLimit = 7;
        random = new Random();
    }

    // 局面の評価値を計算
    private int evaluate(Board board) {
        int phase = board.getCount(0) + board.getCount(1);
        
        int myCorners = countCorners(board, color);
        int oppCorners = countCorners(board, Board.flip(color));
        int cornerScore = 150 * (myCorners - oppCorners);
        
        int myMoves = board.enumerateLegalLocations().size();
        board.pass();
        int oppMoves = board.isLegal() ? board.enumerateLegalLocations().size() : 0;
        board.undo();
        
        int mobilityScore;
        if (phase < 20) {
            mobilityScore = 15 * (myMoves - oppMoves);
        } else if (phase < 50) {
            mobilityScore = 10 * (myMoves - oppMoves);
        } else {
            mobilityScore = 3 * (myMoves - oppMoves);
        }
        
        int myXSquares = countXSquares(board, color);
        int oppXSquares = countXSquares(board, Board.flip(color));
        int xSquareScore = -30 * (myXSquares - oppXSquares);
        
        int myCSquares = countCSquares(board, color);
        int oppCSquares = countCSquares(board, Board.flip(color));
        int cSquareScore = -15 * (myCSquares - oppCSquares);
        
        int myEdgeStability = countEdgeStability(board, color);
        int oppEdgeStability = countEdgeStability(board, Board.flip(color));
        int edgeScore = 8 * (myEdgeStability - oppEdgeStability);
        
        int pieceScore = 0;
        if (phase > 54) {
            pieceScore = 5 * (board.getCount(color) - board.getCount(Board.flip(color)));
        }
        
        return cornerScore + mobilityScore + xSquareScore + cSquareScore + edgeScore + pieceScore;
    }

    // 隅の石の数を数える
    private int countCorners(Board board, int c) {
        int count = 0;
        if (board.get(0, 0) == c) count++;
        if (board.get(7, 0) == c) count++;
        if (board.get(0, 7) == c) count++;
        if (board.get(7, 7) == c) count++;
        return count;
    }

    // X位置の石を数える
    private int countXSquares(Board board, int c) {
        int count = 0;
        if (board.get(0, 0) == -1 && board.get(1, 1) == c) count++;
        if (board.get(7, 0) == -1 && board.get(6, 1) == c) count++;
        if (board.get(0, 7) == -1 && board.get(1, 6) == c) count++;
        if (board.get(7, 7) == -1 && board.get(6, 6) == c) count++;
        return count;
    }

    // C位置の石を数える
    private int countCSquares(Board board, int c) {
        int count = 0;
        if (board.get(0, 0) == -1 && (board.get(0, 1) == c || board.get(1, 0) == c)) {
            if (board.get(0, 1) == c) count++;
            if (board.get(1, 0) == c) count++;
        }
        if (board.get(7, 0) == -1 && (board.get(7, 1) == c || board.get(6, 0) == c)) {
            if (board.get(7, 1) == c) count++;
            if (board.get(6, 0) == c) count++;
        }
        if (board.get(0, 7) == -1 && (board.get(0, 6) == c || board.get(1, 7) == c)) {
            if (board.get(0, 6) == c) count++;
            if (board.get(1, 7) == c) count++;
        }
        if (board.get(7, 7) == -1 && (board.get(7, 6) == c || board.get(6, 7) == c)) {
            if (board.get(7, 6) == c) count++;
            if (board.get(6, 7) == c) count++;
        }
        return count;
    }

    // エッジの安定度を評価
    private int countEdgeStability(Board board, int c) {
        int stability = 0;
        
        for (int i = 0; i < 8; i++) {
            if (board.get(i, 0) == c) stability++;
            if (board.get(i, 7) == c) stability++;
            if (board.get(0, i) == c) stability++;
            if (board.get(7, i) == c) stability++;
        }
        
        return stability;
    }

    // 終局時の評価値を計算
    private int evaluateEnd(Board board, int remainingDepth) {
        int c = board.getCount(color);
        int d = c - board.getCount(Board.flip(color));
        return d > 0 ? c + 1000 * remainingDepth : c - 1000 * remainingDepth;
    }

    // 手の順序をランダム化し優先順位をつける
    private void randomizeLocations(ArrayList<Location> locations) {
        ArrayList<Location> copy = new ArrayList<>(locations);
        locations.clear();
        for (int i = copy.size(); i > 0; i--) {
            locations.add(copy.remove(random.nextInt(i)));
        }
    }

    // αβ法で評価値を最小化
    private int minimize(Board board, int remainingDepth, int alpha, int beta) {
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        ArrayList<Location> locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
        if (locs.isEmpty()) {
            board.pass();
            int score = board.isLegal() ? maximize(board, remainingDepth - 1, alpha, beta) : evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            int score = maximize(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if (score < min) {
                min = score;
            }
            if (min <= alpha) {
                break;
            }
            if (score < beta) {
                beta = score;
            }
            if (timeLimitedFlag && remainingDepth >= 4 && getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return min;
    }

    // αβ法で評価値を最大化
    private int maximize(Board board, int remainingDepth, int alpha, int beta) {
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        ArrayList<Location> locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
        if (locs.isEmpty()) {
            board.pass();
            int score = board.isLegal() ? minimize(board, remainingDepth - 1, alpha, beta) : evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            int score = minimize(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if (score > max) {
                max = score;
                if (remainingDepth == depthLimit) {
                    result = locs.get(i);
                }
            }
            if (max >= beta) {
                break;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (timeLimitedFlag && remainingDepth >= 4 && getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return max;
    }

    @Override
    public Location compute(Board board) {
        result = null;
        maximize(board, depthLimit, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return result;
    }

}
// END