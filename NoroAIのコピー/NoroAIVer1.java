// BEGIN
package j2.review02.s24k0115;

import java.util.ArrayList;
import java.util.Random;

import j2.review02.AI;
import j2.review02.Board;
import j2.review02.Location;

// 課題で作成するリバーシAI

public class NoroAIVer1 extends AI {
    protected final int depthLimit; // 探索の深さ制限
    protected final Random random; // 乱数生成器
    protected Location result; // 計算結果

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
	int pointList[][] = {
			  {120, -20,  20,   5,   5,   5, -20, 120},
			  {-20, -40,  -5,  -5,  -5,  -5, -40, -20},
			  {20,   -5,  15,   3,   3,  15,  -5,  20},
			  {5,     3,   3,   3,   3,   3,   3,   5},
			  {5,     3,   3,   3,   3,   3,   3,   5},
			  {20,   -5,  15,   3,   3,  15,  -5,  20},
			  {-20, -40,  -5,  -5,  -5,  -5, -40, -20},
			  {120, -20,  20,   5,   5,   5, -20, 120}
			};


	int eneyColor = Board.flip(this.color);
	
	
	

    public NoroAIVer1(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        this.depthLimit = 9;
        random = new Random();
    }
    
    public int getPoint(Board board) {

    	
    	
    	int myPoint = 0;
    	int enemyPoint = 0;
    	for(int x = 0; x < 8; x ++) {
    		for(int y = 0; y < 8; y ++) {
    			if(board.get(x, y) == this.color) {
    				myPoint += pointList[x][y];
    			}
    			if(board.get(x, y) == this.eneyColor){
    				enemyPoint += pointList[x][y];
    			}
    		}
    	}
    	return myPoint - enemyPoint;
    }
    
    public int evaluateEdge(Board board) {
    	for(int i = 0; i < 8; i ++) {
    		
    	}
    	return 0;
    }
	
    
    
    protected int evaluateEnd(Board board, int remainingDepth) {//終盤での評価値
        var c = board.getCount(color);//手番
        var d = c - board.getCount(Board.flip(color));//0->1, 1->0
        return d > 0 ? c + 1000 * remainingDepth : c - 1000 * remainingDepth;
    }
    
    protected void randomizeLocations(ArrayList<Location> locations) {
        var copy = new ArrayList<Location>(locations);
        locations.clear();
        for (var i = copy.size(); i > 0; i--) {
            locations.add(copy.remove(random.nextInt(i)));
        }
    }
    protected int evaluate(Board board) {//自分の色の枚数を返す
    	
        return getPoint(board);
    }
    
    protected int minimizeBeta(Board board, int remainingDepth, int alpha, int beta) {
        if (remainingDepth == 0) {
            return evaluate(board);//評価値を返す
        }
        var locs = board.enumerateLegalLocations();//おけるLocationのリスト
        randomizeLocations(locs);
        if (locs.size() == 0) {
            board.pass();//おける場所がないためパスした判定
            var score = board.isLegal() ?//2回連続でパスかの判定
                maximizeAlpha(board, remainingDepth - 1, alpha, beta) ://おけた場合先をscoreにする
                evaluateEnd(board, remainingDepth);//おけない場合評価終了 +か-の大きな値が入る。
            board.undo();// 一つ前の手に戻す
            return score;
        }
        for (var i = 0; i < locs.size(); i++) {//おける場所を全探索
            board.put(locs.get(i));//1手盤面を進める
            var score = maximizeAlpha(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if(beta > score) {
            	beta = score;
            }
            if(alpha >= beta) {
            	return beta;
            }
            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return beta;
    }
    
    protected int maximizeAlpha(Board board, int remainingDepth, int alpha, int beta) {
        if (remainingDepth == 0) {
            return evaluate(board);
        }
        var locs = board.enumerateLegalLocations();
        randomizeLocations(locs);
        if (locs.size() == 0) {
            board.pass();
            var score = board.isLegal() ?
                minimizeBeta(board, remainingDepth - 1, alpha, beta) :
                evaluateEnd(board, remainingDepth);
            board.undo();
            return score;
        }
        for (var i = 0; i < locs.size(); i++) {
            board.put(locs.get(i));
            var score = minimizeBeta(board, remainingDepth - 1, alpha, beta);
            board.undo();
            if(alpha < score) {
            	alpha = score;
                if (remainingDepth == depthLimit) {
                    result = locs.get(i);
                }
            }
            if(alpha >= beta) {
            	return beta;
            }


            if (timeLimitedFlag && remainingDepth >= 4 &&
                    getTime() > 0.95 * TIME_LIMIT) {
                break;
            }
        }
        return alpha;
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    @Override
    public Location compute(Board board) {
        result = null;
    	int alpha0 = -10000;
    	int beta0  =  10000;
        maximizeAlpha(board, depthLimit, alpha0, beta0);
//        System.out.println(result);
        // if(TIME_LIMIT - getTime() < 0) {
        // 	System.out.println("TimeOver"); 
        // }
//        System.out.println(TIME_LIMIT - getTime());
        return result;
    }

}
// END