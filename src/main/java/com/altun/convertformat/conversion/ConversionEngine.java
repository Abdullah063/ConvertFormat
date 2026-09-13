package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;

public interface ConversionEngine {

    ConversionType supportedType();

    String convert(ConversionJob conversionJob);
}
