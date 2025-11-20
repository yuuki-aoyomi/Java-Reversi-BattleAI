// BEGIN
package j2.review02.s24kXXXX;

import java.util.ArrayList;
import java.util.Random;

import j2.review02.AI;
import j2.review02.Board;
import j2.review02.Location;

// 課題で作成するリバーシAI
public class okkuVer4 extends AI {
	protected final int depthLimit = 8; // 探索の深さ制限
    protected final Random random; // 乱数生成器
    protected Location result; // 計算結果
    protected int searchCount;
    private static final int[][] WEIGHTS = {
            { 30, -12, 0, -1, -1, 0, -12, 30 },
            { -12, -15, -3, -3, -3, -3, -15, -12 },
            { 0, -3, 0, -1, -1, 0, -3, 0 },
            { -1, -3, -1, -1, -1, -1, -3, -1 },
            { -1, -3, -1, -1, -1, -1, -3, -1 },
            { 0, -3, 0, -1, -1, 0, -3, 0 },
            { -12, -15, -3, -3, -3, -3, -15, -12 },
            { 30, -12, 0, -1, -1, 0, -12, 30 }
        };

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public okkuVer4(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }
    
    protected int getFrontierScore(Board board, int myColor) {
        int oppColor = Board.flip(myColor);
        int myFrontier = 0;
        int oppFrontier = 0;

        // 8方向の差分
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int piece = board.get(r, c);
                if (piece != -1) {
                    boolean isFrontier = false;
                    // 8方向をチェック
                    for (int i = 0; i < 8; i++) {
                        int nr = r + dr[i];
                        int nc = c + dc[i];
                        // 盤外チェック
                        if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                            // 隣接するマスが空きマスかチェック
                            if (board.get(nr, nc) == -1) {
                                isFrontier = true;
                                break; // 1つでも空きマスに接していれば開放石
                            }
                        }
                    }
                    if (isFrontier) {
                        if (piece == myColor) {
                            myFrontier++;
                        } else {
                            oppFrontier++;
                        }
                    }
                }
            }
        }
        return oppFrontier - myFrontier;
    }

    protected int evaluate(Board board) {
    	int score = 0; // 総合スコア
    	int sumStone = board.getCount(color) + board.getCount(Board.flip(color));
    	int posWeight, mobilityWeight, diffWeight;
    	int frontierWeight;
    	
    	if (sumStone <= 20) {
    	    posWeight = 1;
    	    mobilityWeight = 20;
    	    diffWeight = 0;
    	    frontierWeight = 0;
    	} else if (sumStone <= 48) {
    	    posWeight = 1;
    	    mobilityWeight = 15;
    	    diffWeight = 1;
    	    frontierWeight = 8;
    	} else if (sumStone <= 58) {
    	    posWeight = 1; 
    	    mobilityWeight = 5;
    	    diffWeight = 10;
    	    frontierWeight = 0;
    	} else {
    	    posWeight = 0;
    	    mobilityWeight = 0;
    	    diffWeight = 30;
    	    frontierWeight = 0;
    	}
    	
    	int frontierScore = getFrontierScore(board, color);
    	score += frontierScore * frontierWeight;
    	
    	int posScore = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int stone = board.get(x, y);
                if (stone == color) {
                    posScore += WEIGHTS[x][y];
                } else if (stone == Board.flip(color)) {
                    posScore -= WEIGHTS[x][y];
                }
            }
        }
        
        score += posScore * posWeight;
        
    	ArrayList<Location> myMoves = board.enumerateLegalLocations();
        board.pass();
        ArrayList<Location> oppMoves = board.enumerateLegalLocations();
        board.undo();
        int mobilityScore = myMoves.size() - oppMoves.size();
        
        score += mobilityScore * mobilityWeight;
        
        int stoneDiff = board.getCount(color) - board.getCount(Board.flip(color));
        score += stoneDiff * diffWeight;
        return score;
    }
    
    protected int evaluateEnd(Board board, int remainingDepth) {
        var c = board.getCount(color);
        var d = c - board.getCount(Board.flip(color));
        return d > 0 ? c + 1000 * remainingDepth : c - 1000 * remainingDepth;
    }
    
    protected void randomizeLocations(ArrayList<Location> locations) {
        var copy = new ArrayList<Location>(locations);
        locations.clear();
        for (var i = copy.size(); i > 0; i--) {
            locations.add(copy.remove(random.nextInt(i)));
        }
    }
    
    protected ArrayList<Location> bestLocations(Board board) {
    	var locs = board.enumerateLegalLocations();
    	if (locs.size() == 0) {
    		return locs;
    	}
    	ArrayList<Integer> score = new ArrayList<>();
        for (var i = 0; i < locs.size(); i++) {
            score.add(WEIGHTS[locs.get(i).x()][locs.get(i).y()]);
        }
        for (int i = 0; i < locs.size() - 1; i++) {
            for (int j = i + 1; j < locs.size(); j++) {
                if (score.get(i) < score.get(j)) {
                    int tmpScore = score.get(i);
                    score.set(i, score.get(j));
                    score.set(j, tmpScore);

                    Location tmpLoc = locs.get(i);
                    locs.set(i, locs.get(j));
                    locs.set(j, tmpLoc);
                }
            }
        }
        return locs;
    }
    
    protected int minimize(Board board, int remainingDepth, int alpha, int beta) {
    	searchCount += 1;
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = bestLocations(board);
 
        if (locs.size() == 0) { 
            board.pass();
            var score = board.isLegal() ?
                maximize(board, remainingDepth - 1, alpha, beta) :
                evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        var min = Integer.MAX_VALUE;
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            var score = maximize(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if (score < min) {
                min = score;
            }
            if (min <= alpha) { 
                return min;
            }
            if (min < beta) {
                beta = min;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return min;
    }
    
    protected int maximize(Board board, int remainingDepth, int alpha, int beta) {
    	searchCount += 1;
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = bestLocations(board);
        if (locs.size() == 0) {
            board.pass();
            var score = board.isLegal() ?
                minimize(board, remainingDepth - 1, alpha, beta) :
                evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        var max = Integer.MIN_VALUE;
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            var score = minimize(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if (score > max) {
                max = score;
                if (remainingDepth == depthLimit) {
                    result = locs.get(i);
                }
            }
            if (max >= beta) {
                return max;
            }
            if (max > alpha) {
            	alpha = max;
            }
            
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return max;
    }
    
    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    @Override
    public Location compute(Board board) {
    	result = null;
        maximize(board, depthLimit, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return result;
    }

}
// END
