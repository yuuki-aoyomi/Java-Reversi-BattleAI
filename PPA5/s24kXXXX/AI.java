package j2.review02.s24kXXXX;

import j2.review02.Board;
import j2.review02.Location;

import java.lang.management.ManagementFactory;

// リバーシAIの抽象クラス
public abstract class AI {

    public static final long TIME_LIMIT = 5000000000l; // 時間制限(ナノ秒)

    protected final int color; // プレイヤーの色
    protected final boolean timeLimitedFlag; // 時間制限が設定されているか

    // 色colorのプレイヤーのリバーシAIを生成する．
    // timeLimitedFlagがtrueの場合，時間制限が設定されている．
    protected AI(int color, boolean timeLimitedFlag) {
        this.color = color;
        this.timeLimitedFlag = timeLimitedFlag;
    }

    // 現在の手の計算の開始以降に経過した時間(ナノ秒)を返す．
    public long getTime() {
        return ManagementFactory.getThreadMXBean().
            getCurrentThreadUserTime();
    }

    // 局面boardに対する手を計算し，石を置くマスの座標を返す．
    public abstract Location compute(Board board);

}