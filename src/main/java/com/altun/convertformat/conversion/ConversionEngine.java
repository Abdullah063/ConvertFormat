package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;

import java.util.Set;

public interface ConversionEngine {

    Set<ConversionType> supportedTypes();

    String convert(ConversionJob conversionJob);
}
