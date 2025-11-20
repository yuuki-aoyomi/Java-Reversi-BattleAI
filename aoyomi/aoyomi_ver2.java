// BEGIN
package j2.review02.s24k0120;

import java.util.ArrayList;
import java.util.Random;

import j2.review02.AI;
import j2.review02.Board;
import j2.review02.Location;

// 課題で作成するリバーシAI
public class MyAI extends AI {

	protected final int depthLimit; // 探索の深さ制限
	protected final Random random; // 乱数生成器
    protected Location result; // 計算結果
    private static final int[][] WEIGHT = {
    	    {120, -20,  20,   5,   5,  20, -20, 120},
    	    {-20, -40,  -5,  -5,  -5,  -5, -40, -20},
    	    { 20,  -5,  15,   3,   3,  15,  -5,  20},
    	    {  5,  -5,   3,   3,   3,   3,  -5,   5},
    	    {  5,  -5,   3,   3,   3,   3,  -5,   5},
    	    { 20,  -5,  15,   3,   3,  15,  -5,  20},
    	    {-20, -40,  -5,  -5,  -5,  -5, -40, -20},
    	    {120, -20,  20,   5,   5,  20, -20, 120},
    	}; //重み
    
    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public MyAI(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        this.depthLimit = 10;
        random = new Random();
    }

    // 局面boardを評価する．
    protected int evaluate(Board board) {
        int myColor = color;
        int enemyColor = Board.flip(color);

        // Mobility（合法手の多さ）評価
        int myMobility = board.enumerateLegalLocations().size();

        // passして相手番にして合法手数カウント
        board.pass();
        int enemyMobility = board.enumerateLegalLocations().size();
        board.undo();

        int mobilityScore = 5 * (myMobility - enemyMobility); 
        // ← 重みは調整可能


        // 位置評価：Positional Weight
        // get(x,y) があるので本格的な位置評価テーブルが使える！
        int positionalScore = 0;

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int cell = board.get(x, y);
                if (cell == myColor) {
                    positionalScore += WEIGHT[y][x];
                } else if (cell == enemyColor) {
                    positionalScore -= WEIGHT[y][x];
                }
            }
        }

        // -----------------------------
        // ③ 合成して返す
        // -----------------------------
        return positionalScore + mobilityScore;
    }
    
    // 葉の局面boardを評価する．
    // 追加の引数として残りの深さremainingDepthを受け取る．
    protected int evaluateEnd(Board board, int remainingDepth) {
        var c = board.getCount(color);
        var d = c - board.getCount(Board.flip(color));
        return d > 0 ? c + 1000 * remainingDepth : c - 1000 * remainingDepth;
    }
    
    // マスのリストlocationsの要素をランダムに並べ替える．
    protected void randomizeLocations(ArrayList<Location> locations) {
        var copy = new ArrayList<Location>(locations);
        locations.clear();
        for (var i = copy.size(); i > 0; i--) {
            locations.add(copy.remove(random.nextInt(i)));
        }
    }

    // 評価値を最小化する．
    // 引数として局面board，残りの深さremainingDepthを受け取る．
    protected int minimize(Board board, int remainingDepth, int alpha, int beta) {
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
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
            if (min < beta) {
            	beta = min;
            }
            if (beta<=alpha) {
            	break;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return min;
    }

    // 評価値を最大化する．
    // 引数として局面board，残りの深さremainingDepthを受け取る．
    protected int maximize(Board board, int remainingDepth, int alpha, int beta) {
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
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
            if (max > alpha) {
            	alpha = max;
            }
            if (beta <= alpha) {
            	break;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return max;
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    // 評価値に石数を用いたMinimax法によって手を選ぶ．
    @Override
    public Location compute(Board board) {
        result = null;
        maximize(board, depthLimit, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return result;
    }

}
// END