package com.altun.convertformat.conversion;

import com.altun.convertformat.entities.ConversionJob;

public record ConversionJobCreation(ConversionJob job, String downloadToken) {
}
