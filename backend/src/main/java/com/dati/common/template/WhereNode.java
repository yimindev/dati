package com.dati.common.template;

import java.util.List;

/** Smart WHERE block: {{#where}}...{{/where}}. 首个 AND/OR 裁剪，全部跳过时 WHERE 消失。 */
record WhereNode(List<Node> body) implements Node {}
