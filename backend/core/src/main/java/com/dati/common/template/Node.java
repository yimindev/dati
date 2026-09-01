package com.dati.common.template;

/** Sealed interface for all AST nodes. Only the four V1 node types are permitted. */
sealed interface Node permits TextNode, VarNode, IfNode, WhereNode {}
