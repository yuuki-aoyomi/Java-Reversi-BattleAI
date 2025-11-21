package j2.review02;

import java.util.ArrayList;
import java.util.Random;

import j2.review02.s24k0136.AI;

public class A5_ver2 extends AI {
    // 探索で使用する無限大の値
    private static final int INF = 1_000_000;

    // 時間制限の何%まで使うか (92% = 5秒中4.6秒まで)
    private static final double TIME_MARGIN = 0.92;

    // 各マスの価値を表す重みテーブル
    // 角(120)は最も価値が高く、その隣(-20, -40)は危険
    private static final int[][] POSITION_WEIGHTS = {
        {120, -20, 20, 5, 5, 20, -20, 120},
        {-20, -40, -5, -5, -5, -5, -40, -20},
        {20, -5, 15, 3, 3, 15, -5, 20},
        {5, -5, 3, 3, 3, 3, -5, 5},
        {5, -5, 3, 3, 3, 3, -5, 5},
        {20, -5, 15, 3, 3, 15, -5, 20},
        {-20, -40, -5, -5, -5, -5, -40, -20},
        {120, -20, 20, 5, 5, 20, -20, 120}
    };

    //インスタンス変数 

    private final Random random;      // ランダム選択用
    private long startTime;           // 探索開始時刻
    private boolean timeOver;         // 時間切れフラグ
    private Location bestMoveFound;   // 現在見つかっている最善手

    public A5_ver2(int color, boolean timeLimitedFlag) {
        super(color, timeLimitedFlag);
        random = new Random();
    }

    /*
    反復深化探索を使って最善手を見つける
    Args:
       board : 現在の盤面
    Returns:
       bestMoveFound : 選択した手
     */
    @Override
    public Location compute(Board board) {
        // 合法手を取得
        ArrayList<Location> legalMoves = board.enumerateLegalLocations();
        if (legalMoves.isEmpty()) {
            return null; // 打つ手がない
        }

        // 初期化
        startTime = getTime();
        timeOver = false;

        // フォールバック用：評価関数で最も良さそうな手を選ぶ
        bestMoveFound = selectBestMoveByEvaluation(legalMoves, board);

        // 探索の深さを決定（盤面の空きマス数に応じて調整）
        int maxDepth = calculateSearchDepth(board);

        // 反復深化探索：深さ1から順に深くしていく
        // 時間切れになる前に浅い探索結果を保持しておく
        for (int depth = 1; depth <= maxDepth; depth++) {
            // この深さでの最善手を探す
            Location moveAtDepth = searchAtDepth(board, depth);

            // 時間切れで中断された場合、前回の結果を使う
            if (timeOver) {
                break;
            }

            // 完了した探索の結果を採用
            if (moveAtDepth != null) {
                bestMoveFound = moveAtDepth;
            }

            // 時間が迫っていたら探索を打ち切る
            if (timeLimitedFlag && isTimeRunningOut()) {
                break;
            }
        }

        return bestMoveFound;
    }

    /*
    指定した深さで探索を実行
    Args:
        board : 盤面
        depth : 探索深さ
    Returns:
        bestMove : この深さでの最善手
     */
    private Location searchAtDepth(Board board, int depth) {
        Location bestMove = null;
        int bestScore = -INF;

        // 全ての合法手を取得して並び替え
        ArrayList<Location> moves = board.enumerateLegalLocations();
        sortMovesByPriority(board, moves);

        // 各手を試す
        for (Location move : moves) {
            // 手を実行
            board.put(move);

            // 相手の番での評価を計算（符号を反転）
            int score = -alphaBetaSearch(board, depth - 1, -INF, INF, false);

            // 手を戻す
            board.undo();

            // 時間切れチェック
            if (timeOver) {
                return bestMove; // これまでの最善手を返す
            }

            // より良い手が見つかったら更新
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    /*
    αβ探索を行い、木を探索して局面の評価値を返す
    Args:
        board        : 現在の盤面
        depth        : 残りの探索深さ
        alpha        : α値（これより悪い手は探索しない）
        beta         : β値（これより良い手は探索しない）
        passedBefore : 直前にパスがあったか
    Returns:
        bestValue : この局面の評価値
     */
    private int alphaBetaSearch(Board board, int depth, int alpha, int beta,
            boolean passedBefore) {
        // 時間切れチェック
        if (timeLimitedFlag && isTimeRunningOut()) {
            timeOver = true;
            return evaluatePosition(board);
        }

        // 深さ0に到達：葉ノードなので評価
        if (depth == 0) {
            return evaluatePosition(board);
        }

        // 合法手を取得
        ArrayList<Location> moves = board.enumerateLegalLocations();

        // 打つ手がない場合
        if (moves.isEmpty()) {
            // 両者ともパスなら終局
            if (passedBefore) {
                return evaluateEndGame(board);
            }
            // パスして相手のターンへ
            board.pass();
            int value = -alphaBetaSearch(board, depth - 1, -beta, -alpha, true);
            board.undo();
            return value;
        }

        // 手を並び替えて効率的に探索
        sortMovesByPriority(board, moves);

        // 最善値を探す
        int bestValue = -INF;

        for (Location move : moves) {
            // 手を実行
            board.put(move);

            // 再帰的に探索（相手の視点なので符号反転）
            int value = -alphaBetaSearch(board, depth - 1, -beta, -alpha, false);

            // 手を戻す
            board.undo();

            // 時間切れで中断
            if (timeOver) {
                return bestValue == -INF ? evaluatePosition(board) : bestValue;
            }

            // より良い値が見つかったら更新
            if (value > bestValue) {
                bestValue = value;
            }

            // α値を更新
            if (bestValue > alpha) {
                alpha = bestValue;
            }

            // β刈り：これ以上探索する必要なし
            if (alpha >= beta) {
                break;
            }
        }

        return bestValue;
    }

    /*
    局面を評価する（中盤・序盤用）
    複数の要素を組み合わせて総合評価値を計算
    Args:
        board : 評価する盤面
    Returns:
        int : 評価値（大きいほど有利）
     */
    private int evaluatePosition(Board board) {
        int myColor = color;
        int opponentColor = Board.flip(color);
        int totalStones = board.getCount(0) + board.getCount(1);

        //1. 位置評価 
        // 各マスの戦略的価値の合計
        int positional = calculatePositionalScore(board, myColor)
                       - calculatePositionalScore(board, opponentColor);

        //2. 機動力（打てる手の数）
        // 序盤・中盤は多く、終盤は少ない方が有利な場合もある
        int mobility = calculateMobility(board, myColor)
                     - calculateMobility(board, opponentColor);

        //3. 角の確保 
        // 角は絶対に取られないため非常に重要
        int corners = 1000 * (countCorners(board, myColor)
                            - countCorners(board, opponentColor));

        //4. 辺の確保 
        // 辺も比較的安定している
        int edges = 50 * (countEdges(board, myColor)
                        - countEdges(board, opponentColor));

        //5. 石の数 
        // 終盤に向かうほど重要度が増す
        int pieceWeight = (totalStones >= 50) ? 10 : 2;
        int pieces = (board.getCount(myColor) - board.getCount(opponentColor))
                   * pieceWeight;

        //重み付けして合計 
        // 序盤は機動力重視、中盤以降は位置と確定石重視
        int mobilityWeight = (totalStones < 30) ? 80 : 40;

        return positional + mobility * mobilityWeight + corners + edges + pieces;
    }

    /*
      終局時の評価（勝敗が決まった状態）
      Args:
        board : 盤面
      Returns:
        int : 評価値(石差 × 10000)
     */
    private int evaluateEndGame(Board board) {
        int myStones = board.getCount(color);
        int opponentStones = board.getCount(Board.flip(color));
        int diff = myStones - opponentStones;

        return diff * 10_000;
    }

    /*
    位置評価の計算
    POSITION_WEIGHTSテーブルに基づいて石の配置を評価
     */
    private int calculatePositionalScore(Board board, int targetColor) {
        int score = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (board.get(x, y) == targetColor) {
                    score += POSITION_WEIGHTS[y][x];
                }
            }
        }
        return score;
    }

    /*
    機動力の計算
    その色が打てる合法手の数
     */
    private int calculateMobility(Board board, int targetColor) {
        if (board.getCurrentColor() == targetColor) {
            // 現在の手番ならそのまま数える
            return board.enumerateLegalLocations().size();
        } else {
            // 相手の手番ならパスして数える
            board.pass();
            int count = board.enumerateLegalLocations().size();
            board.undo();
            return count;
        }
    }

    /*
    手を優先度順に並び替える
    良い手を先に探索することでαβ枝刈りが効率的になる
     */
    private void sortMovesByPriority(Board board, ArrayList<Location> moves) {
        // 選択ソートで並び替え（シンプルだが十分高速）
        for (int i = 0; i < moves.size() - 1; i++) {
            int bestIndex = i;
            int bestScore = evaluateMove(board, moves.get(i));

            for (int j = i + 1; j < moves.size(); j++) {
                int score = evaluateMove(board, moves.get(j));
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = j;
                }
            }

            // 交換
            if (bestIndex != i) {
                Location temp = moves.get(i);
                moves.set(i, moves.get(bestIndex));
                moves.set(bestIndex, temp);
            }
        }
    }

    /*
    個別の手の価値を簡易評価
    角 > 辺 > 内側、ただしX位置（角の隣）は避ける
     */
    private int evaluateMove(Board board, Location move) {
        int score = POSITION_WEIGHTS[move.y()][move.x()];

        // 角は最優先
        if (isCorner(move)) {
            score += 5000;
        }
        // X位置（角の隣の斜め）は角が空いている時は危険
        else if (isXSquare(move) && isCornerEmpty(board, move)) {
            score -= 3000;
        }
        // 辺は有利
        else if (isEdge(move)) {
            score += 500;
        }

        return score;
    }

    /*
    フォールバック用：評価関数だけで最善手を選ぶ
     */
    private Location selectBestMoveByEvaluation(ArrayList<Location> moves, Board board) {
        Location best = moves.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (Location move : moves) {
            int score = evaluateMove(board, move);
            if (score > bestScore || (score == bestScore && random.nextBoolean())) {
                bestScore = score;
                best = move;
            }
        }

        return best;
    }

    /*
    角にある石の数を数える
     */
    private int countCorners(Board board, int targetColor) {
        int count = 0;
        if (board.get(0, 0) == targetColor) count++;
        if (board.get(7, 0) == targetColor) count++;
        if (board.get(0, 7) == targetColor) count++;
        if (board.get(7, 7) == targetColor) count++;
        return count;
    }

    /*
    辺にある石の数を数える
     */
    private int countEdges(Board board, int targetColor) {
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if (board.get(i, 0) == targetColor) count++;
            if (board.get(i, 7) == targetColor) count++;
            if (board.get(0, i) == targetColor) count++;
            if (board.get(7, i) == targetColor) count++;
        }
        return count;
    }

    /*
    角かどうか
     */
    private boolean isCorner(Location loc) {
        return (loc.x() == 0 || loc.x() == 7) && (loc.y() == 0 || loc.y() == 7);
    }

    /*
    辺かどうか
     */
    private boolean isEdge(Location loc) {
        return loc.x() == 0 || loc.x() == 7 || loc.y() == 0 || loc.y() == 7;
    }

    /*
    X位置（角の斜め隣）かどうか
     */
    private boolean isXSquare(Location loc) {
        return (loc.x() == 1 || loc.x() == 6) && (loc.y() == 1 || loc.y() == 6);
    }

    /*
    その位置に対応する角が空いているか
     */
    private boolean isCornerEmpty(Board board, Location loc) {
        int cornerX = (loc.x() < 4) ? 0 : 7;
        int cornerY = (loc.y() < 4) ? 0 : 7;
        return board.get(cornerX, cornerY) == -1;
    }

    /*
    残りの空きマス数に応じて探索深さを決定
     */
    private int calculateSearchDepth(Board board) {
        int totalStones = board.getCount(0) + board.getCount(1);
        int emptySquares = 64 - totalStones;

        if (emptySquares <= 10) {
            // 終盤：完全読み切り
            return emptySquares;
        } else if (emptySquares <= 16) {
            // 終盤寄り：深めに読む
            return 10;
        } else if (emptySquares <= 30) {
            // 中盤：標準的な深さ
            return 8;
        } else {
            // 序盤：浅めでOK
            return 6;
        }
    }

    /*
    時間が迫っているかチェック
     */
    private boolean isTimeRunningOut() {
        return getTime() - startTime > TIME_LIMIT * TIME_MARGIN;
    }

}
