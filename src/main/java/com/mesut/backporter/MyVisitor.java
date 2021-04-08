package com.mesut.backporter;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;

public class MyVisitor extends ASTVisitor {

    @Override
    public boolean visit(VariableDeclarationExpression node) {
        return super.visit(node);
    }
}
