package macrobase.kernel;

import macrobase.kernel.BandwidthSelector;
import macrobase.util.TinyDataSource;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class BandwidthSelectorTest {
    @Test
    public void testVerySimple() throws Exception {
        List<double[]> data = new TinyDataSource().get();
        BandwidthSelector s = new BandwidthSelector();
        double[] bw = s.findBandwidth(data);
        for (int i=0; i < bw.length; i++) {
            assertThat(bw[i], lessThan(5.0));
            assertThat(bw[i], greaterThan(2.0));
        }
    }
}
