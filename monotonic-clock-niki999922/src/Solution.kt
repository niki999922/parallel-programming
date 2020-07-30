class Solution : MonotonicClock {
    private var c0_1 by RegularInt(0)
    private var c0_2 by RegularInt(0)
    private var c0_3 by RegularInt(0)
    private var c1_1 by RegularInt(0)
    private var c1_2 by RegularInt(0)
    private var c1_3 by RegularInt(0)

    override fun write(time: Time) {
        c1_1 = time.d1
        c1_2 = time.d2
        c1_3 = time.d3
        c0_3 = time.d3
        c0_2 = time.d2
        c0_1 = time.d1
    }

    override fun read(): Time {
        val c0s_1 = c0_1
        val c0s_2 = c0_2
        val c0s_3 = c0_3
        val c1s_3 = c1_3
        val c1s_2 = c1_2
        val c1s_1 = c1_1
        if (c1s_1 == c0s_1 && c1s_2 == c0s_2 && c1s_3 == c0s_3) {
            return Time(c0s_1, c0s_2, c0s_3)
        }
        if (c1s_1 == c0s_1 && c1s_2 == c0s_2 && c0s_3 < c1s_3) {
            return Time(c0s_1, c0s_2, c1s_3)
        }
        if (c0s_1 < c1s_1) {
            return Time(c1s_1, 0, 0)
        }
        if (c1s_1 == c0s_1 && c0s_2 < c1s_2) {
            return Time(c0s_1, c1s_2, 0)
        }
        return Time(c1s_1, c1s_2, c1s_3)

    }
}