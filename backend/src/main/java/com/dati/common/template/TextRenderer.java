package com.dati.common.template;

import java.util.Map;

public interface TextRenderer {
    String render(CompiledTemplate compiled, Map<String, Object> params);
}
