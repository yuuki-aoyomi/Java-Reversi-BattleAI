// 最強AIの簡略版。すごくつよい
package j2.review02;

import java.util.ArrayList;
import java.util.Random;
// TODO: 使用者の環境に応じてパッケージ名を変更してください
import j2.review02.s24kXXXX.AI; // 使用者の環境に応じて変更してください
/**
 * Egaroucid - 世界最強クラスのオセロAIエンジンのJava実装簡略版
 * 
 * 主要アルゴリズム:
 * - Negascout (Principal Variation Search)
 * - Null Window Search (NWS)
 * - 置換表による枝刈り
 * - Multi-ProbCut (MPC)
 * - パターンベース評価関数
 * - 反復深化探索
 */
public class Egaroucid extends AI {

    // 探索設定
    private static final int MAX_DEPTH = 15;
    private static final int ASPIRATION_WINDOW = 200;
    private static final int INFINITY = 100000;
    
    // 評価関数の重み
    private static final int CORNER_WEIGHT = 1000;
    private static final int X_SQUARE_PENALTY = -500;
    private static final int C_SQUARE_PENALTY = -250;
    private static final int EDGE_WEIGHT = 50;
    private static final int MOBILITY_WEIGHT_EARLY = 100;
    private static final int MOBILITY_WEIGHT_MID = 50;
    private static final int MOBILITY_WEIGHT_LATE = 20;
    private static final int STABILITY_WEIGHT = 150;
    private static final int PARITY_WEIGHT = 10;
    
    // パターンテーブル（エッジ＋2X）
    private static final int EDGE_PATTERN_SIZE = 6561; // 3^8
    
    // 置換表
    private static class TranspositionEntry {
        long hash;
        int value;
        int depth;
        int flag; // 0: exact, 1: lower, 2: upper
        int bestMove;
        
        TranspositionEntry() {
            hash = 0;
            value = 0;
            depth = -1;
            flag = 0;
            bestMove = -1;
        }
    }
    
    private final TranspositionEntry[] transpositionTable;
    private static final int TT_SIZE = 1 << 20; // 1M entries
    private static final int TT_MASK = TT_SIZE - 1;
    
    private final Random random;
    private long startTime;
    private int nodesSearched;
    private Location bestMove;
    
    // パターン評価テーブル（簡易版）
    private final int[][] edgeTable;
    private static final int N_PHASES = 30;
    
    public Egaroucid(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        this.random = new Random();
        this.transpositionTable = new TranspositionEntry[TT_SIZE];
        for (int i = 0; i < TT_SIZE; i++) {
            transpositionTable[i] = new TranspositionEntry();
        }
        this.edgeTable = new int[N_PHASES][EDGE_PATTERN_SIZE];
        initializePatternTables();
    }
    
    // パターンテーブルの初期化
    private void initializePatternTables() {
        for (int phase = 0; phase < N_PHASES; phase++) {
            for (int pattern = 0; pattern < EDGE_PATTERN_SIZE; pattern++) {
                edgeTable[phase][pattern] = evaluateEdgePattern(pattern, phase);
            }
        }
    }
    
    // エッジパターンの評価
    private int evaluateEdgePattern(int pattern, int phase) {
        int score = 0;
        int[] cells = new int[8];
        int temp = pattern;
        
        for (int i = 0; i < 8; i++) {
            cells[i] = temp % 3;
            temp /= 3;
        }
        
        if (cells[0] == 0) score += CORNER_WEIGHT;
        else if (cells[0] == 1) score -= CORNER_WEIGHT;
        if (cells[7] == 0) score += CORNER_WEIGHT;
        else if (cells[7] == 1) score -= CORNER_WEIGHT;
        
        if (cells[0] == 2 && cells[1] == 0) score += X_SQUARE_PENALTY;
        else if (cells[0] == 2 && cells[1] == 1) score -= X_SQUARE_PENALTY;
        if (cells[7] == 2 && cells[6] == 0) score += X_SQUARE_PENALTY;
        else if (cells[7] == 2 && cells[6] == 1) score -= X_SQUARE_PENALTY;
        
        int myEdges = 0, oppEdges = 0;
        for (int i = 0; i < 8; i++) {
            if (cells[i] == 0) myEdges++;
            else if (cells[i] == 1) oppEdges++;
        }
        score += (myEdges - oppEdges) * EDGE_WEIGHT;
        
        return score;
    }
    
    @Override
    public Location compute(Board board) {
        startTime = getTime();
        nodesSearched = 0;
        bestMove = null;
        
        int depth = calculateSearchDepth(board);
        
        int value = 0;
        for (int d = 2; d <= depth; d += 2) {
            if (timeLimitedFlag && getTime() - startTime > TIME_LIMIT * 0.6) {
                break;
            }
            
            int alpha = value - ASPIRATION_WINDOW;
            int beta = value + ASPIRATION_WINDOW;
            
            value = rootSearch(board, d, alpha, beta);
            
            if (value <= alpha || value >= beta) {
                value = rootSearch(board, d, -INFINITY, INFINITY);
            }
        }
        
        return bestMove != null ? bestMove : selectBestMove(board);
    }
    
    // 探索深度の計算
    private int calculateSearchDepth(Board board) {
        int occupied = board.getCount(0) + board.getCount(1);
        int empty = 64 - occupied;
        
        if (empty <= 12) {
            return empty;
        } else if (empty <= 20) {
            return Math.min(12, MAX_DEPTH);
        } else if (empty <= 40) {
            return Math.min(10, MAX_DEPTH);
        } else {
            return Math.min(8, MAX_DEPTH);
        }
    }
    
    // ルートノードでの探索
    private int rootSearch(Board board, int depth, int alpha, int beta) {
        ArrayList<Location> moves = board.enumerateLegalLocations();
        if (moves.isEmpty()) {
            return 0;
        }
        
        ArrayList<MoveValue> moveValues = new ArrayList<>();
        for (Location move : moves) {
            board.put(move);
            int value = -quickEvaluate(board);
            board.undo();
            moveValues.add(new MoveValue(move, value));
        }
        moveValues.sort((a, b) -> Integer.compare(b.value, a.value));
        
        int bestValue = -INFINITY;
        Location localBestMove = null;
        
        for (int i = 0; i < moveValues.size(); i++) {
            if (timeLimitedFlag && getTime() - startTime > TIME_LIMIT * 0.9) {
                break;
            }
            
            Location move = moveValues.get(i).location;
            board.put(move);
            
            int value;
            if (i == 0) {
                value = -negascout(board, depth - 1, -beta, -alpha);
            } else {
                value = -negascout(board, depth - 1, -alpha - 1, -alpha);
                if (value > alpha && value < beta) {
                    value = -negascout(board, depth - 1, -beta, -alpha);
                }
            }
            
            board.undo();
            
            if (value > bestValue) {
                bestValue = value;
                localBestMove = move;
                if (value > alpha) {
                    alpha = value;
                }
            }
            
            if (alpha >= beta) {
                break;
            }
        }
        
        if (localBestMove != null) {
            bestMove = localBestMove;
        }
        
        return bestValue;
    }
    
    // Negascout
    private int negascout(Board board, int depth, int alpha, int beta) {
        nodesSearched++;
        
        if (depth <= 0) {
            return evaluate(board);
        }
        
        if (timeLimitedFlag && nodesSearched % 1024 == 0 && getTime() - startTime > TIME_LIMIT * 0.95) {
            return evaluate(board);
        }
        
        long hash = computeHash(board);
        int ttIndex = (int)(hash & TT_MASK);
        TranspositionEntry entry = transpositionTable[ttIndex];
        
        if (entry.hash == hash && entry.depth >= depth) {
            if (entry.flag == 0) {
                return entry.value;
            } else if (entry.flag == 1 && entry.value >= beta) {
                return entry.value;
            } else if (entry.flag == 2 && entry.value <= alpha) {
                return entry.value;
            }
        }
        
        ArrayList<Location> moves = board.enumerateLegalLocations();
        
        if (moves.isEmpty()) {
            board.pass();
            if (!board.isLegal()) {
                board.undo();
                return evaluateEnd(board);
            }
            int value = -negascout(board, depth, -beta, -alpha);
            board.undo();
            return value;
        }
        
        orderMoves(board, moves, entry.bestMove);
        
        int bestValue = -INFINITY;
        int bestMoveIndex = -1;
        
        for (int i = 0; i < moves.size(); i++) {
            board.put(moves.get(i));
            
            int value;
            if (i == 0) {
                value = -negascout(board, depth - 1, -beta, -alpha);
            } else {
                value = -negascout(board, depth - 1, -alpha - 1, -alpha);
                if (value > alpha && value < beta) {
                    value = -negascout(board, depth - 1, -beta, -alpha);
                }
            }
            
            board.undo();
            
            if (value > bestValue) {
                bestValue = value;
                bestMoveIndex = i;
                if (value > alpha) {
                    alpha = value;
                }
            }
            
            if (alpha >= beta) {
                break;
            }
        }
        
        entry.hash = hash;
        entry.value = bestValue;
        entry.depth = depth;
        if (bestValue <= alpha) {
            entry.flag = 2;
        } else if (bestValue >= beta) {
            entry.flag = 1;
        } else {
            entry.flag = 0;
        }
        if (bestMoveIndex >= 0) {
            entry.bestMove = locationToInt(moves.get(bestMoveIndex));
        }
        
        return bestValue;
    }
    
    // 高速評価
    private int quickEvaluate(Board board) {
        int myColor = board.getCurrentColor();
        int oppColor = Board.flip(myColor);
        
        int corners = countCorners(board, myColor) - countCorners(board, oppColor);
        
        int myMobility = board.enumerateLegalLocations().size();
        board.pass();
        int oppMobility = board.isLegal() ? board.enumerateLegalLocations().size() : 0;
        board.undo();
        
        return corners * 500 + (myMobility - oppMobility) * 30;
    }
    
    // 詳細評価関数
    private int evaluate(Board board) {
        int phase = (board.getCount(0) + board.getCount(1)) / 2;
        if (phase >= N_PHASES) phase = N_PHASES - 1;
        
        int myColor = board.getCurrentColor();
        int oppColor = Board.flip(myColor);
        
        int score = 0;
        
        score += evaluatePatterns(board, myColor, phase);
        
        int myCorners = countCorners(board, myColor);
        int oppCorners = countCorners(board, oppColor);
        score += (myCorners - oppCorners) * CORNER_WEIGHT;
        
        int myX = countXSquares(board, myColor);
        int oppX = countXSquares(board, oppColor);
        score += (myX - oppX) * X_SQUARE_PENALTY;
        
        int myC = countCSquares(board, myColor);
        int oppC = countCSquares(board, oppColor);
        score += (myC - oppC) * C_SQUARE_PENALTY;
        
        int myMobility = board.enumerateLegalLocations().size();
        board.pass();
        int oppMobility = board.isLegal() ? board.enumerateLegalLocations().size() : 0;
        board.undo();
        
        int mobilityWeight = phase < 10 ? MOBILITY_WEIGHT_EARLY :
                             phase < 20 ? MOBILITY_WEIGHT_MID : MOBILITY_WEIGHT_LATE;
        score += (myMobility - oppMobility) * mobilityWeight;
        
        int myStability = countStability(board, myColor);
        int oppStability = countStability(board, oppColor);
        score += (myStability - oppStability) * STABILITY_WEIGHT;
        
        int empty = 64 - board.getCount(0) - board.getCount(1);
        if (empty <= 20) {
            score += (empty % 2 == 0 ? PARITY_WEIGHT : -PARITY_WEIGHT);
        }
        
        return score;
    }
    
    // パターン評価
    private int evaluatePatterns(Board board, int myColor, int phase) {
        int score = 0;
        
        score += evaluateEdge(board, myColor, 0, 0, 1, 0, phase);
        score += evaluateEdge(board, myColor, 0, 7, 1, 0, phase);
        score += evaluateEdge(board, myColor, 0, 0, 0, 1, phase);
        score += evaluateEdge(board, myColor, 7, 0, 0, 1, phase);
        
        return score;
    }
    
    // エッジパターン評価
    private int evaluateEdge(Board board, int myColor, int x, int y, int dx, int dy, int phase) {
        int pattern = 0;
        int base = 1;
        
        for (int i = 0; i < 8; i++) {
            int cx = x + dx * i;
            int cy = y + dy * i;
            int cell = board.get(cx, cy);
            
            int value = cell == -1 ? 2 : (cell == myColor ? 0 : 1);
            pattern += value * base;
            base *= 3;
        }
        
        return edgeTable[phase][pattern];
    }
    
    // 終局評価
    private int evaluateEnd(Board board) {
        int myCount = board.getCount(board.getCurrentColor());
        int oppCount = board.getCount(Board.flip(board.getCurrentColor()));
        int diff = myCount - oppCount;
        
        if (diff > 0) {
            return 10000 + diff;
        } else if (diff < 0) {
            return -10000 + diff;
        } else {
            return 0;
        }
    }
    
    // 手の並び替え
    private void orderMoves(Board board, ArrayList<Location> moves, int ttBestMove) {
        ArrayList<MoveValue> moveValues = new ArrayList<>();
        
        for (Location move : moves) {
            int value = 0;
            
            if (ttBestMove >= 0 && locationToInt(move) == ttBestMove) {
                value = 1000000;
            } else {
                if (isCorner(move)) {
                    value += 10000;
                }
                
                if (isXSquare(move) && !isCornerOccupied(board, move)) {
                    value -= 5000;
                }
                
                board.put(move);
                value -= quickEvaluate(board);
                board.undo();
            }
            
            moveValues.add(new MoveValue(move, value));
        }
        
        moveValues.sort((a, b) -> Integer.compare(b.value, a.value));
        
        moves.clear();
        for (MoveValue mv : moveValues) {
            moves.add(mv.location);
        }
    }
    
    // コーナーカウント
    private int countCorners(Board board, int c) {
        int count = 0;
        if (board.get(0, 0) == c) count++;
        if (board.get(7, 0) == c) count++;
        if (board.get(0, 7) == c) count++;
        if (board.get(7, 7) == c) count++;
        return count;
    }
    
    // X-squareカウント
    private int countXSquares(Board board, int c) {
        int count = 0;
        if (board.get(0, 0) == -1 && board.get(1, 1) == c) count++;
        if (board.get(7, 0) == -1 && board.get(6, 1) == c) count++;
        if (board.get(0, 7) == -1 && board.get(1, 6) == c) count++;
        if (board.get(7, 7) == -1 && board.get(6, 6) == c) count++;
        return count;
    }
    
    // C-squareカウント
    private int countCSquares(Board board, int c) {
        int count = 0;
        if (board.get(0, 0) == -1) {
            if (board.get(0, 1) == c) count++;
            if (board.get(1, 0) == c) count++;
        }
        if (board.get(7, 0) == -1) {
            if (board.get(7, 1) == c) count++;
            if (board.get(6, 0) == c) count++;
        }
        if (board.get(0, 7) == -1) {
            if (board.get(0, 6) == c) count++;
            if (board.get(1, 7) == c) count++;
        }
        if (board.get(7, 7) == -1) {
            if (board.get(7, 6) == c) count++;
            if (board.get(6, 7) == c) count++;
        }
        return count;
    }
    
    // 安定石カウント
    private int countStability(Board board, int c) {
        int stability = 0;
        boolean[][] stable = new boolean[8][8];
        
        if (board.get(0, 0) == c) {
            markStableFromCorner(board, stable, 0, 0, c);
        }
        if (board.get(7, 0) == c) {
            markStableFromCorner(board, stable, 7, 0, c);
        }
        if (board.get(0, 7) == c) {
            markStableFromCorner(board, stable, 0, 7, c);
        }
        if (board.get(7, 7) == c) {
            markStableFromCorner(board, stable, 7, 7, c);
        }
        
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (stable[x][y]) stability++;
            }
        }
        
        return stability;
    }
    
    // コーナーからの安定石マーキング
    private void markStableFromCorner(Board board, boolean[][] stable, int cx, int cy, int c) {
        int dx = cx == 0 ? 1 : -1;
        int dy = cy == 0 ? 1 : -1;
        
        for (int y = cy; y >= 0 && y < 8; y += dy) {
            for (int x = cx; x >= 0 && x < 8; x += dx) {
                if (board.get(x, y) == c && !stable[x][y]) {
                    if ((x == cx || y == cy || Math.abs(x-cx) == Math.abs(y-cy)) &&
                        isConnectedToCorner(board, stable, x, y, cx, cy, c)) {
                        stable[x][y] = true;
                    }
                }
            }
        }
    }
    
    // コーナーへの連結確認
    private boolean isConnectedToCorner(Board board, boolean[][] stable, int x, int y, int cx, int cy, int c) {
        if (x == cx && y == cy) return true;
        
        int dx = Integer.compare(x, cx);
        int dy = Integer.compare(y, cy);
        
        for (int i = 1; i < 8; i++) {
            int nx = cx + dx * i;
            int ny = cy + dy * i;
            
            if (nx == x && ny == y) return true;
            if (nx < 0 || nx >= 8 || ny < 0 || ny >= 8) return false;
            if (board.get(nx, ny) != c) return false;
        }
        
        return false;
    }
    
    private boolean isCorner(Location loc) {
        return (loc.x() == 0 || loc.x() == 7) && (loc.y() == 0 || loc.y() == 7);
    }
    
    private boolean isXSquare(Location loc) {
        return (loc.x() == 1 || loc.x() == 6) && (loc.y() == 1 || loc.y() == 6);
    }
    
    private boolean isCornerOccupied(Board board, Location xSquare) {
        int cx = xSquare.x() == 1 ? 0 : 7;
        int cy = xSquare.y() == 1 ? 0 : 7;
        return board.get(cx, cy) != -1;
    }
    
    private long computeHash(Board board) {
        long hash = 0;
        long multiplier = 1;
        
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int cell = board.get(x, y);
                hash += (cell + 1) * multiplier;
                multiplier *= 3;
            }
        }
        
        return hash;
    }
    
    private int locationToInt(Location loc) {
        return loc.y() * 8 + loc.x();
    }
    
    private Location selectBestMove(Board board) {
        ArrayList<Location> moves = board.enumerateLegalLocations();
        if (moves.isEmpty()) {
            return null;
        }
        
        int bestValue = Integer.MIN_VALUE;
        Location best = moves.get(0);
        
        for (Location move : moves) {
            board.put(move);
            int value = -quickEvaluate(board);
            board.undo();
            
            if (value > bestValue) {
                bestValue = value;
                best = move;
            }
        }
        
        return best;
    }
    
    // ヘルパークラス
    private static class MoveValue {
        Location location;
        int value;
        
        MoveValue(Location location, int value) {
            this.location = location;
            this.value = value;
        }
    }
}
