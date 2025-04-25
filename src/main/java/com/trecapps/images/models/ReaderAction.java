package com.trecapps.images.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReaderAction {
    @NotNull ReaderActionType action;
    @NotNull List<String> readers;
}
