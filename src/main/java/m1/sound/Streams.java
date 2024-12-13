/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.function.BiConsumer;

import m1.emu.Attotime;
import m1.emu.Attotime.Atime;
import m1.emu.Timer;

import static java.lang.System.getLogger;


/** sound_stream */
public class Streams {

    private static final Logger logger = getLogger(Streams.class.getName());

    public int sample_rate;
    public int new_sample_rate;
    public int gain;
    public long attoseconds_per_sample;
    public int max_samples_per_update;
    public final int inputs;
    public final int outputs;
    public int output_sampindex;
    public int output_base_sampindex;
    public final int[][] streaminput;
    public final int[][] streamoutput;
    // updatedelegate
    private final BiConsumer<Integer, Integer> updatecallback;

    // updatedelegate
    public Streams(int _sample_rate, int _inputs, int _outputs, BiConsumer<Integer, Integer> callback) {
logger.log(Level.INFO, "sampleRate: %d, inputs: %d, outputs: %d, callbacl: %s".formatted(_sample_rate, _inputs, _outputs, callback));
        sample_rate = _sample_rate;
        inputs = _inputs;
        outputs = _outputs;
        attoseconds_per_sample = Attotime.ATTOSECONDS_PER_SECOND / sample_rate;
        max_samples_per_update = (int) ((Sound.update_attoseconds + attoseconds_per_sample - 1) / attoseconds_per_sample);
        output_base_sampindex = -max_samples_per_update;
        streaminput = new int[inputs][];
logger.log(Level.INFO, "attoseconds_per_sample: %d, max_samples_per_update: %d, output_base_sampindex: %d".formatted(attoseconds_per_sample, max_samples_per_update, output_base_sampindex));
        for (int i = 0; i < inputs; i++) {
            streaminput[i] = new int[max_samples_per_update];
        }
        streamoutput = new int[outputs][];
        for (int i = 0; i < outputs; i++) {
            streamoutput[i] = new int[5 * max_samples_per_update * 100]; // TODO vavi
logger.log(Level.INFO, "streamoutput[%d]: %d".formatted(i, streamoutput[i].length));
        }
        updatecallback = callback;
    }

    public void stream_update() {
        int update_sampindex = time_to_sampindex(Timer.getCurrentTime());
logger.log(Level.INFO, "update_sampindex: %d, output_sampindex: %d".formatted(update_sampindex, output_sampindex));
        int offset, samples;
        samples = update_sampindex - output_sampindex;
        if (samples > 0) {
            offset = output_sampindex - output_base_sampindex;
            updatecallback.accept(offset, samples);
        }
        output_sampindex = update_sampindex;
    }

    public void adjuststream(boolean second_tick) {
        int i, j;
        int output_bufindex = output_sampindex - output_base_sampindex;
        if (second_tick) {
            output_sampindex -= sample_rate;
            output_base_sampindex -= sample_rate;
        }
        if (output_bufindex > 3 * max_samples_per_update) {
            int samples_to_lose = output_bufindex - max_samples_per_update;
            for (i = 0; i < streamoutput.length; i++) {
                for (j = 0; j < max_samples_per_update; j++) {
                    streamoutput[i][j] = streamoutput[i][samples_to_lose + j];
                }
            }
            output_base_sampindex += samples_to_lose;
        }
    }

    private int time_to_sampindex(Atime time) {
logger.log(Level.INFO, "time.attoseconds: %d, attoseconds_per_sample: %d".formatted(time.attoseconds, attoseconds_per_sample));
        int sample = (int) (time.attoseconds / attoseconds_per_sample);
        if (time.seconds > Sound.last_update_second) {
            sample += sample_rate;
        }
        if (time.seconds < Sound.last_update_second) {
            sample -= sample_rate;
        }
        return sample;
    }

    public void updatesamplerate() {
        int i;
        if (new_sample_rate != 0) {
            int old_rate = sample_rate;
            sample_rate = new_sample_rate;
            new_sample_rate = 0;
            attoseconds_per_sample = (long) 1e18 / sample_rate;
            max_samples_per_update = (int) ((Sound.update_attoseconds + attoseconds_per_sample - 1) / attoseconds_per_sample);
            output_sampindex = (int) ((long) output_sampindex * (long) sample_rate / old_rate);
            output_base_sampindex = output_sampindex - max_samples_per_update;
            for (i = 0; i < outputs; i++) {
                Arrays.fill(streamoutput[i], 0, max_samples_per_update, 0);
            }
        }
    }
}
