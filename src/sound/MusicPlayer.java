package sound;

import javax.sound.midi.*;

/**
 * Plays a looping MIDI background track for dungeon atmosphere.
 * The track is generated programmatically as a dark, minor-key
 * ambient piece inspired by Noita's cave aesthetic.
 * Uses the Java javax.sound.midi API from the standard library.
 */
public class MusicPlayer {

    /** The MIDI sequencer used to play the generated track */
    private Sequencer sequencer;
    /** Current volume 0-127 */
    private int volume = 80;
    /** Whether music is currently playing */
    private boolean playing = false;

    // MIDI channel indices
    private static final int CH_BASS   = 0;
    private static final int CH_MELODY = 1;
    private static final int CH_PAD    = 2;
    private static final int CH_DRUM   = 9; // Channel 10 is always drums in GM

    // Pulses per quarter note
    private static final int PPQ = 24;
    // Tempo in BPM (slow, atmospheric)
    private static final int BPM = 72;
    // Microseconds per quarter note derived from BPM
    private static final int TEMPO_US = 60_000_000 / BPM;

    /**
     * Builds and starts the MIDI background music.
     * Silently ignores errors if MIDI is unavailable on the system.
     */
    public void startMusic() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();

            Sequence seq = new Sequence(Sequence.PPQ, PPQ);
            buildTrack(seq);

            sequencer.setSequence(seq);
            sequencer.setTempoInBPM(BPM);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            playing = true;
        } catch (Exception e) {
            System.err.println("MIDI unavailable: " + e.getMessage());
        }
    }

    /**
     * Stops and releases MIDI resources.
     */
    public void stopMusic() {
        if (sequencer != null && sequencer.isOpen()) {
            sequencer.stop();
            sequencer.close();
        }
        playing = false;
    }

    /**
     * Sets the overall playback volume.
     *
     * @param vol Volume level 0 (silent) to 127 (maximum)
     */
    public void setVolume(int vol) {
        this.volume = MathUtils.clampInt(vol, 0, 127);
        if (sequencer != null && sequencer.isOpen()) {
            try {
                MidiChannel[] channels = ((Synthesizer) MidiSystem.getSynthesizer()).getChannels();
                for (MidiChannel ch : channels) {
                    ch.controlChange(7, this.volume);
                }
            } catch (Exception ignored) {}
        }
    }

    /** @return True if the music sequencer is currently playing */
    public boolean isPlaying() { return playing; }

    /**
     * Builds all MIDI tracks onto the sequence.
     * The piece uses an A minor chord progression: Am – F – C – G
     * played slowly with bass, atmospheric pad chords, and a simple melody.
     *
     * @param seq The MIDI sequence to add tracks to
     */
    private void buildTrack(Sequence seq) throws InvalidMidiDataException {
        Track track = seq.createTrack();

        // Set tempo
        MetaMessage tempo = new MetaMessage();
        byte[] td = {
            (byte)((TEMPO_US >> 16) & 0xFF),
            (byte)((TEMPO_US >> 8)  & 0xFF),
            (byte)( TEMPO_US        & 0xFF)
        };
        tempo.setMessage(0x51, td, 3);
        track.add(new MidiEvent(tempo, 0));

        // Instrument assignments
        setInstrument(track, CH_BASS,   32, 0);   // Acoustic bass
        setInstrument(track, CH_MELODY, 73, 0);   // Flute
        setInstrument(track, CH_PAD,    91, 0);   // Pad 4 (choir)

        // 4-bar chord progression, looped twice = 8 bars total
        // A minor = A C E | F major = F A C | C major = C E G | G major = G B D
        int[] chordRoots  = {57, 53, 60, 55}; // A3, F3, C4, G3
        int[][] chords    = {
            {57, 60, 64},  // Am: A C E
            {53, 57, 60},  // F:  F A C
            {60, 64, 67},  // C:  C E G
            {55, 59, 62}   // G:  G B D
        };
        int[] bassNotes   = {33, 29, 36, 31}; // A2, F2, C2, G2

        // Melody motif over the 4 chords (8th-note rhythm)
        int[][] melodyNotes = {
            {69, 72, 71, 69},  // Bar 1 (Am)
            {65, 67, 65, 64},  // Bar 2 (F)
            {64, 67, 65, 64},  // Bar 3 (C)
            {62, 64, 62, 60}   // Bar 4 (G)
        };

        int beatsPerBar = 4;
        int ticksPerBeat = PPQ;
        int barTicks = beatsPerBar * ticksPerBeat;
        int numLoops = 2;

        for (int loop = 0; loop < numLoops; loop++) {
            for (int bar = 0; bar < 4; bar++) {
                long barStart = (long)(loop * 4 + bar) * barTicks;

                // Bass note: whole note per bar
                addNote(track, CH_BASS, bassNotes[bar], volume - 20,
                        barStart, barTicks - 2);

                // Pad chord: whole note per bar
                for (int note : chords[bar]) {
                    addNote(track, CH_PAD, note, volume - 35,
                            barStart, barTicks - 2);
                }

                // Melody: four quarter notes per bar
                for (int beat = 0; beat < 4; beat++) {
                    int mel = melodyNotes[bar][beat];
                    if (mel > 0) {
                        addNote(track, CH_MELODY, mel, volume - 10,
                                barStart + beat * ticksPerBeat,
                                ticksPerBeat - 4);
                    }
                }
            }
        }

        // End-of-track marker
        MetaMessage eot = new MetaMessage();
        eot.setMessage(0x2F, new byte[]{}, 0);
        track.add(new MidiEvent(eot, numLoops * 4 * barTicks + 1));
    }

    /**
     * Adds a MIDI program change event to set the instrument for a channel.
     *
     * @param track      Track to add the event to
     * @param channel    MIDI channel (0-15)
     * @param instrument General MIDI instrument number (0-127)
     * @param tick       Tick position for the event
     */
    private void setInstrument(Track track, int channel, int instrument, long tick)
            throws InvalidMidiDataException {
        ShortMessage msg = new ShortMessage();
        msg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, instrument, 0);
        track.add(new MidiEvent(msg, tick));
    }

    /**
     * Adds a note-on followed by note-off event to the track.
     *
     * @param track    Target track
     * @param channel  MIDI channel
     * @param pitch    MIDI note number (0-127)
     * @param velocity Note velocity (0-127)
     * @param startTick Tick at which to start the note
     * @param duration  Duration in ticks
     */
    private void addNote(Track track, int channel, int pitch, int velocity,
                         long startTick, long duration) throws InvalidMidiDataException {
        ShortMessage on = new ShortMessage();
        on.setMessage(ShortMessage.NOTE_ON, channel, pitch, velocity);
        track.add(new MidiEvent(on, startTick));

        ShortMessage off = new ShortMessage();
        off.setMessage(ShortMessage.NOTE_OFF, channel, pitch, 0);
        track.add(new MidiEvent(off, startTick + duration));
    }

    /** Simple int clamp without importing MathUtils to avoid circular deps */
    private static class MathUtils {
        static int clampInt(int v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}
