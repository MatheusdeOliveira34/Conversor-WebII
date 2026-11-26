package br.edu.fileconverter.converter;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.exception.ConversionException;

/**
 * Conversor de um par origem → destino. Para adicionar uma conversão (RNF10), basta criar
 * uma classe {@code @Component} que implementa esta interface: o {@link ConverterRegistry}
 * a encontra sozinho, sem mudar controller nem service.
 */
public interface DocumentConverter {

    FileFormat source();

    FileFormat target();

    byte[] convert(byte[] input) throws ConversionException;
}
