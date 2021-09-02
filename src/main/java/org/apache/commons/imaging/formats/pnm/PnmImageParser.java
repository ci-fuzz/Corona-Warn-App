/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.imaging.formats.pnm;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.ImageParser;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.common.ByteOrder;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.common.ImageBuilder;
import org.apache.commons.imaging.common.bytesource.ByteSource;
import org.apache.commons.imaging.palette.PaletteFactory;
import org.apache.commons.imaging.util.IoUtils;

public class PnmImageParser extends ImageParser {
    private static final String DEFAULT_EXTENSION = ".pnm";
    private static final String ACCEPTED_EXTENSIONS[] = { ".pbm", ".pgm",
            ".ppm", ".pnm", ".pam" };
    public static final String PARAM_KEY_PNM_RAWBITS = "PNM_RAWBITS";
    public static final String PARAM_VALUE_PNM_RAWBITS_YES = "YES";
    public static final String PARAM_VALUE_PNM_RAWBITS_NO = "NO";

    public PnmImageParser() {
        super.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        // setDebug(true);
    }

    @Override
    public String getName() {
        return "Pbm-Custom";
    }

    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Override
    protected String[] getAcceptedExtensions() {
        return ACCEPTED_EXTENSIONS;
    }

    @Override
    protected ImageFormat[] getAcceptedTypes() {
        return new ImageFormat[] {
                ImageFormats.PBM, //
                ImageFormats.PGM, //
                ImageFormats.PPM, //
                ImageFormats.PNM,
                ImageFormats.PAM
        };
    }

    private FileInfo readHeader(final InputStream is) throws ImageReadException,
            IOException {
        final byte identifier1 = readByte("Identifier1", is, "Not a Valid PNM File");
        final byte identifier2 = readByte("Identifier2", is, "Not a Valid PNM File");

        if (identifier1 != PnmConstants.PNM_PREFIX_BYTE) {
            throw new ImageReadException("PNM file has invalid prefix byte 1");
        }
        
        final WhiteSpaceReader wsr = new WhiteSpaceReader(is);
        
        if (identifier2 == PnmConstants.PBM_TEXT_CODE ||
                identifier2 == PnmConstants.PBM_RAW_CODE ||
                identifier2 == PnmConstants.PGM_TEXT_CODE ||
                identifier2 == PnmConstants.PGM_RAW_CODE ||
                identifier2 == PnmConstants.PPM_TEXT_CODE ||
                identifier2 == PnmConstants.PPM_RAW_CODE) {
            
            final int width = Integer.parseInt(wsr.readtoWhiteSpace());
            final int height = Integer.parseInt(wsr.readtoWhiteSpace());
    
            if (identifier2 == PnmConstants.PBM_TEXT_CODE) {
                return new PbmFileInfo(width, height, false);
            } else if (identifier2 == PnmConstants.PBM_RAW_CODE) {
                return new PbmFileInfo(width, height, true);
            } else if (identifier2 == PnmConstants.PGM_TEXT_CODE) {
                final int maxgray = Integer.parseInt(wsr.readtoWhiteSpace());
                return new PgmFileInfo(width, height, false, maxgray);
            } else if (identifier2 == PnmConstants.PGM_RAW_CODE) {
                final int maxgray = Integer.parseInt(wsr.readtoWhiteSpace());
                return new PgmFileInfo(width, height, true, maxgray);
            } else if (identifier2 == PnmConstants.PPM_TEXT_CODE) {
                final int max = Integer.parseInt(wsr.readtoWhiteSpace());
                return new PpmFileInfo(width, height, false, max);
            } else if (identifier2 == PnmConstants.PPM_RAW_CODE) {
                final int max = Integer.parseInt(wsr.readtoWhiteSpace());
                return new PpmFileInfo(width, height, true, max);
            } else {
                throw new ImageReadException("PNM file has invalid header.");
            }
        } else if (identifier2 == PnmConstants.PAM_RAW_CODE) {
            int width = -1;
            boolean seenWidth = false;
            int height = -1;
            boolean seenHeight = false;
            int depth = -1;
            boolean seenDepth = false;
            int maxVal = -1;
            boolean seenMaxVal = false;
            final StringBuilder tupleType = new StringBuilder();
            boolean seenTupleType = false;
            
            // Advance to next line
            wsr.readLine();
            String line;
            while ((line = wsr.readLine()) != null) {
                line = line.trim();
                if (line.charAt(0) == '#') {
                    continue;
                }
                final StringTokenizer tokenizer = new StringTokenizer(line, " ", false);
                final String type = tokenizer.nextToken();
                if ("WIDTH".equals(type)) {
                    seenWidth = true;
                    width = Integer.parseInt(tokenizer.nextToken());
                } else if ("HEIGHT".equals(type)) {
                    seenHeight = true;
                    height = Integer.parseInt(tokenizer.nextToken());
                } else if ("DEPTH".equals(type)) {
                    seenDepth = true;
                    depth = Integer.parseInt(tokenizer.nextToken());
                } else if ("MAXVAL".equals(type)) {
                    seenMaxVal = true;
                    maxVal = Integer.parseInt(tokenizer.nextToken());
                } else if ("TUPLTYPE".equals(type)) {
                    seenTupleType = true;
                    tupleType.append(tokenizer.nextToken());
                } else if ("ENDHDR".equals(type)) {
                    break;
                } else {
                    throw new ImageReadException("Invalid PAM file header type " + type);
                }
            }
            
            if (!seenWidth) {
                throw new ImageReadException("PAM header has no WIDTH");
            } else if (!seenHeight) {
                throw new ImageReadException("PAM header has no HEIGHT");
            } else if (!seenDepth) {
                throw new ImageReadException("PAM header has no DEPTH");
            } else if (!seenMaxVal) {
                throw new ImageReadException("PAM header has no MAXVAL");
            } else if (!seenTupleType) {
                throw new ImageReadException("PAM header has no TUPLTYPE");
            }
            
            return new PamFileInfo(width, height, depth, maxVal, tupleType.toString());
        } else {
            throw new ImageReadException("PNM file has invalid prefix byte 2");
        }
    }

    private FileInfo readHeader(final ByteSource byteSource)
            throws ImageReadException, IOException {
        InputStream is = null;
        boolean canThrow = false;
        try {
            is = byteSource.getInputStream();

            final FileInfo ret = readHeader(is);
            canThrow = true;
            return ret;
        } finally {
            IoUtils.closeQuietly(canThrow, is);
        }
    }

    @Override
    public byte[] getICCProfileBytes(final ByteSource byteSource, final Map<String,Object> params)
            throws ImageReadException, IOException {
        return null;
    }

    @Override
    public Dimension getImageSize(final ByteSource byteSource, final Map<String,Object> params)
            throws ImageReadException, IOException {
        final FileInfo info = readHeader(byteSource);

        if (info == null) {
            throw new ImageReadException("PNM: Couldn't read Header");
        }

        return new Dimension(info.width, info.height);
    }

    public byte[] embedICCProfile(final byte image[], final byte profile[]) {
        return null;
    }

    @Override
    public boolean embedICCProfile(final File src, final File dst, final byte profile[]) {
        return false;
    }

    @Override
    public IImageMetadata getMetadata(final ByteSource byteSource, final Map<String,Object> params)
            throws ImageReadException, IOException {
        return null;
    }

    @Override
    public ImageInfo getImageInfo(final ByteSource byteSource, final Map<String,Object> params)
            throws ImageReadException, IOException {
        final FileInfo info = readHeader(byteSource);

        if (info == null) {
            throw new ImageReadException("PNM: Couldn't read Header");
        }

        final List<String> Comments = new ArrayList<String>();

        final int BitsPerPixel = info.getBitDepth() * info.getNumComponents();
        final ImageFormat Format = info.getImageType();
        final String FormatName = info.getImageTypeDescription();
        final String MimeType = info.getMIMEType();
        final int NumberOfImages = 1;
        final boolean isProgressive = false;

        // boolean isProgressive = (fPNGChunkIHDR.InterlaceMethod != 0);
        //
        final int PhysicalWidthDpi = 72;
        final float PhysicalWidthInch = (float) ((double) info.width / (double) PhysicalWidthDpi);
        final int PhysicalHeightDpi = 72;
        final float PhysicalHeightInch = (float) ((double) info.height / (double) PhysicalHeightDpi);

        final String FormatDetails = info.getImageTypeDescription();

        final boolean isTransparent = info.hasAlpha();
        final boolean usesPalette = false;

        final int ColorType = info.getColorType();
        final String compressionAlgorithm = ImageInfo.COMPRESSION_ALGORITHM_NONE;

        final ImageInfo result = new ImageInfo(FormatDetails, BitsPerPixel, Comments,
                Format, FormatName, info.height, MimeType, NumberOfImages,
                PhysicalHeightDpi, PhysicalHeightInch, PhysicalWidthDpi,
                PhysicalWidthInch, info.width, isProgressive, isTransparent,
                usesPalette, ColorType, compressionAlgorithm);

        return result;
    }

    @Override
    public boolean dumpImageFile(final PrintWriter pw, final ByteSource byteSource)
            throws ImageReadException, IOException {
        pw.println("pnm.dumpImageFile");

        final ImageInfo imageData = getImageInfo(byteSource);
        if (imageData == null) {
            return false;
        }

        imageData.toString(pw, "");

        pw.println("");

        return true;
    }

    @Override
    public BufferedImage getBufferedImage(final ByteSource byteSource, final Map<String,Object> params)
            throws ImageReadException, IOException {
        InputStream is = null;
        boolean canThrow = false;
        try {
            is = byteSource.getInputStream();

            final FileInfo info = readHeader(is);

            final int width = info.width;
            final int height = info.height;

            final boolean hasAlpha = info.hasAlpha();
            final ImageBuilder imageBuilder = new ImageBuilder(width, height,
                    hasAlpha);
            info.readImage(imageBuilder, is);

            final BufferedImage ret = imageBuilder.getBufferedImage();
            canThrow = true;
            return ret;
        } finally {
            IoUtils.closeQuietly(canThrow, is);
        }
    }

    @Override
    public void writeImage(final BufferedImage src, final OutputStream os, Map<String,Object> params)
            throws ImageWriteException, IOException {
        PnmWriter writer = null;
        boolean useRawbits = true;
        final boolean hasAlpha = new PaletteFactory().hasTransparency(src);

        if (params != null) {
            final Object useRawbitsParam = params.get(PARAM_KEY_PNM_RAWBITS);
            if (useRawbitsParam != null) {
                if (useRawbitsParam.equals(PARAM_VALUE_PNM_RAWBITS_NO)) {
                    useRawbits = false;
                }
            }

            final Object subtype = params.get(PARAM_KEY_FORMAT);
            if (subtype != null) {
                if (subtype.equals(ImageFormats.PBM)) {
                    writer = new PbmWriter(useRawbits);
                } else if (subtype.equals(ImageFormats.PGM)) {
                    writer = new PgmWriter(useRawbits);
                } else if (subtype.equals(ImageFormats.PPM)) {
                    writer = new PpmWriter(useRawbits);
                } else if (subtype.equals(ImageFormats.PAM)) { 
                    writer = new PamWriter();
                }
            }
        }

        if (writer == null) {
            if (hasAlpha) {
                writer = new PamWriter();
            } else {   
                writer = new PpmWriter(useRawbits);
            }
        }

        // make copy of params; we'll clear keys as we consume them.
        if (params != null) {
            params = new HashMap<String,Object>(params);
        } else {
            params = new HashMap<String,Object>();
        }

        // clear format key.
        if (params.containsKey(PARAM_KEY_FORMAT)) {
            params.remove(PARAM_KEY_FORMAT);
        }
        
        if (!params.isEmpty()) {
            final Object firstKey = params.keySet().iterator().next();
            throw new ImageWriteException("Unknown parameter: " + firstKey);
        }

        writer.writeImage(src, os, params);
    }

    /**
     * Extracts embedded XML metadata as XML string.
     * <p>
     * 
     * @param byteSource
     *            File containing image data.
     * @param params
     *            Map of optional parameters, defined in ImagingConstants.
     * @return Xmp Xml as String, if present. Otherwise, returns null.
     */
    @Override
    public String getXmpXml(final ByteSource byteSource, final Map<String,Object> params)
            throws ImageReadException, IOException {
        return null;
    }
}
