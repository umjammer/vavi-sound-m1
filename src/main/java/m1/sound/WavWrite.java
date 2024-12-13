/*
 * https://www.codeproject.com/Tips/646359/M-NET
 */

package m1.sound;

import java.io.IOException;

import dotnet4j.io.BinaryWriter;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.SeekOrigin;


public class WavWrite {

    public static FileStream waveFile = null;
    private static BinaryWriter writer = null;
    private static int sampleCount = 0;

    public static void createSoundFile(String filename) {
        waveFile = new FileStream(filename, FileMode.Create);
        writer = new BinaryWriter(waveFile);
        // Hereiswherethefilewillbecreated.A
        // wavefileisaRIFFfile,whichhaschunks
        // ofdatathatdescribewhatthefilecontains.
        // AwaveRIFFfileisputtogetherlikethis:
        // The12byteRIFFchunkisconstructedlikethis:
        // Bytes0-3:　'R''I''F''F'
        // Bytes4-7:　Lengthoffile,minusthefirst8bytesoftheRIFFdescription.
        // (4bytesfor"WAVE"+24bytesforformatchunklength+
        // 8bytesfordatachunkdescription+actualsampledatasize.)
        // Bytes8-11:'W''A''V''E'
        // The24byteFORMATchunkisconstructedlikethis:
        // Bytes0-3:'f''m''t'''
        // Bytes4-7:Theformatchunklength.Thisisalways16.
        // Bytes8-9:Filepadding.Always1.
        // Bytes10-11:Numberofchannels.Either1formono,　or2forstereo.
        // Bytes12-15:Samplerate.
        // Bytes16-19:Numberofbytespersecond.
        // Bytes20-21:Bytespersample.1for8bitmono,2for8bitstereoor
        // 16bitmono,4for16bitstereo.
        // Bytes22-23:Numberofbitspersample.
        // TheDATAchunkisconstructedlikethis:
        // Bytes0-3:'d''a''t''a'
        // Bytes4-7:Lengthofdata,inbytes.
        // Bytes8-:Actualsampledata.
        char[] ChunkRiff = {'R', 'I', 'F', 'F'};
        char[] ChunkType = {'W', 'A', 'V', 'E'};
        char[] ChunkFmt = {'f', 'm', 't', ' '};
        char[] ChunkData = {'d', 'a', 't', 'a'};
        short shPad = 1; // Filepadding
        int nFormatChunkLength = 0x10; // Formatchunklength.
        int nLength = 0; // Filelength,minusfirst8bytesofRIFFdescription.Thiswillbefilledinlater.
        sampleCount = 0;
        // RIFF
        writer.write(ChunkRiff);
        writer.write(nLength);
        // WAVE
        writer.write(ChunkType);
        writer.write(ChunkFmt);
        writer.write(nFormatChunkLength);
        writer.write(shPad);
        writer.write((short) 2);
        writer.write(48000);
        writer.write(192000);
        writer.write((short) 4);
        writer.write((short) 16);
        //data
        writer.write(ChunkData);
        writer.write(0);
    }

    public static void closeSoundFile() throws IOException {
        writer.seek(4, SeekOrigin.Begin);
        writer.write(sampleCount + 36);
        writer.seek(40, SeekOrigin.Begin);
        writer.write(sampleCount);
        writer.close();
        waveFile.close();
        writer = null;
        waveFile = null;
    }

    public static void addData16(byte[] bb1, int length) {
        writer.write(bb1, 0, length);
        sampleCount += length;
        writer.flush();
    }
}
