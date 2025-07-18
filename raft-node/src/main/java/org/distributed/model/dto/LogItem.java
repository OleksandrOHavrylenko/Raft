package org.distributed.model.dto;

/**
 * @author Oleksandr Havrylenko
 **/
public record LogItem(int id, String message, long term) {
}
