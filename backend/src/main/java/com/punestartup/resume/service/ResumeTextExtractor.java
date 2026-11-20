package com.punestartup.resume.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.apache.tika.config.OutputLimits;
import org.apache.tika.config.TimeoutLimits;
import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.TikaTimeoutException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class ResumeTextExtractor {

    private final int maxCharacters;
    private final Duration parseTimeout;

    public ResumeTextExtractor(
            @Value("${app.resume.max-parsed-characters:200000}") int maxCharacters,
            @Value("${app.resume.parse-timeout:PT30S}") Duration parseTimeout) {
        this.maxCharacters = maxCharacters;
        this.parseTimeout = parseTimeout;
    }

    public String extract(InputStream input, String filename) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        BodyContentHandler handler = new BodyContentHandler(maxCharacters);
        ParseContext context = new ParseContext();
        TimeoutLimits timeout = new TimeoutLimits(parseTimeout.toMillis(),
                Math.min(parseTimeout.toMillis(), 10000));
        timeout.setThrowOnDeadline(true);
        context.set(TimeoutLimits.class, timeout);
        context.set(OutputLimits.class, new OutputLimits(maxCharacters, false, 100,
                20L * 1024 * 1024, 100));
        Parser parser = new AutoDetectParser();
        context.set(Parser.class, parser);
        try (TikaInputStream tikaInput = TikaInputStream.get(input, metadata)) {
            parser.parse(tikaInput, handler, metadata, context);
        } catch (TikaTimeoutException ex) {
            throw new ResumeParseException("PARSE_TIMEOUT", "Resume parsing timed out", ex);
        } catch (SAXException ex) {
            throw new ResumeParseException("TEXT_TOO_LONG", "Resume text is too large", ex);
        } catch (TikaException ex) {
            throw new ResumeParseException("PARSE_FAILED", "Resume could not be parsed", ex);
        } catch (IOException ex) {
            throw new ResumeParseException("STORAGE_READ_FAILED", "Resume could not be read", ex);
        }
        return handler.toString();
    }
}
