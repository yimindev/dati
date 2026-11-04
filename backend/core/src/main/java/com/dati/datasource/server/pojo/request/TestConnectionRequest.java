package com.dati.datasource.server.pojo.request;

import com.dati.db.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for connection probes. Length limits mirror the storage column widths;
 * the password stays optional because some databases accept password-less accounts.
 * <p>
 * When {@code id} points to a saved data source and no password is supplied, the stored
 * connection is probed instead; the request's connection fields must then match the saved ones.
 */
@Data
public class TestConnectionRequest {

    private String id;

    @NotNull
    private DbType type;

    @NotBlank
    @Size(max = 256)
    private String jdbcUrl;

    @NotBlank
    @Size(max = 64)
    private String username;

    @Size(max = 512)
    private String password;

}
