package de.ethinking.gradle.contenttype

/**
 * Exception occured while processing the content type XML fragments.
 * @author jho
 */
class ContentTypeProcessingException extends RuntimeException {
    ContentTypeProcessingException(String message) {
        super(message)
    }

    ContentTypeProcessingException(String message, Throwable cause) {
        super(message, cause)
    }
}
