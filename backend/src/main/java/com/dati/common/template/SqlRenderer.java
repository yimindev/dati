package com.dati.common.template;

import java.util.Map;

public interface SqlRenderer {
    PreparedSql render(CompiledTemplate compiled, Map<String, Object> params);
}
