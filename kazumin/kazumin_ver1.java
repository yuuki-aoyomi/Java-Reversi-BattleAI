// BEGIN
package j2.review02.s24k0134;

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
    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    public MyAI(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        this.depthLimit = 6;
        random = new Random();
    }
    private static final int MOBILITY_WEIGHT = 10;//可動性（Mobility）とは？
    											//可動性＝その局面で指せる合法手数の多さ
    private static final int[][] CELL_WEIGHT = { //重みの追加
    	{15, 11, 9, 9, 9, 9, 11, 15},
    	{11, 14, 9, 7, 7, 9, 14, 11},
    	{9, 9, 10, 6, 6, 10, 9, 9},
    	{9, 7, 6, 7, 7, 6, 7, 9},
    	{9, 7, 6, 7, 7, 6, 7, 9},
    	{9, 9, 10, 6, 6, 10, 9, 9},
    	{11, 14, 9, 7, 7, 9, 14, 11},
    	{15, 11, 9, 9, 9, 9, 11, 15}
    };
    
 // 局面boardを評価する．
    protected int evaluate(Board board) {
    	var score = 0;
    	//位置重み
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int c = board.get(x, y);
                if (c == color) {
                    score += CELL_WEIGHT[x][y];
                } else if (c == Board.flip(color)) {
                    score -= CELL_WEIGHT[x][y];
                }
            }
        }
        //合法手数差
		//序盤〜中盤：合法手は 2〜10 個
		//終盤：誰でも読める（探索が届く）
		//「探索のため」にモビリティを使うわけではない。
		//minimax の計算量とは無関係
        
        Board tmp = new Board(board);
        int myMoves, oppMoves;
        if (tmp.getCurrentColor() == color) {
            myMoves = tmp.enumerateLegalLocations().size();
            tmp.pass();
            oppMoves = tmp.enumerateLegalLocations().size();
        } else {
            // 盤面の手番が相手のとき
            oppMoves = tmp.enumerateLegalLocations().size();
            tmp.pass();
            myMoves = tmp.enumerateLegalLocations().size();
        }
        score += MOBILITY_WEIGHT * (myMoves - oppMoves);
        
        return score;
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
            
            min = Math.min(min, score);
            beta = Math.min(beta, min);
            if (beta <= alpha) { // 枝刈り
                break;
            }
//            if (score < min) {
//                min = score;
//            }
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
            alpha = Math.max(alpha, max); // α更新

            if (alpha >= beta) { // ★枝刈り！
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
    @Override
    public Location compute(Board board) {
        result = null;
        maximize(board, depthLimit, Integer.MIN_VALUE, Integer.MAX_VALUE);
        return result;
    }

}
// END