package fuzz_targets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.awt.image.BufferedImage;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.common.bytesource.ByteSourceArray;

import org.apache.commons.imaging.formats.bmp.BmpImageParser;
import org.apache.commons.imaging.formats.dcx.DcxImageParser;
import org.apache.commons.imaging.formats.gif.GifImageParser;
import org.apache.commons.imaging.formats.icns.IcnsImageParser;
import org.apache.commons.imaging.formats.ico.IcoImageParser;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.formats.pcx.PcxImageParser;
import org.apache.commons.imaging.formats.png.PngImageParser;
import org.apache.commons.imaging.formats.pnm.PnmImageParser;
import org.apache.commons.imaging.formats.psd.PsdImageParser;
import org.apache.commons.imaging.formats.rgbe.RgbeImageParser;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.wbmp.WbmpImageParser;
import org.apache.commons.imaging.formats.xbm.XbmImageParser;
import org.apache.commons.imaging.formats.xpm.XpmImageParser;

import org.apache.commons.imaging.ImageParser;
import java.awt.image.RasterFormatException;



public class AllFormatsFuzzer {
	public static void fuzzerTestOneInput(byte[] input) {	
		try {
			ImageParser[] p = ImageParser.getAllImageParsers();
			for (int i = 0; i < p.length; i++) {
				BufferedImage image = p[i].getBufferedImage(new ByteSourceArray(input), new HashMap<>());
			 }
		} catch (IOException | ImageReadException | RasterFormatException e) {
			return;
		}
	}
}