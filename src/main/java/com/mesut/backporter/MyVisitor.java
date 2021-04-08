package com.mesut.backporter;

import org.eclipse.jdt.core.dom.*;

public class MyVisitor extends ASTVisitor {

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        System.out.println("varrrr");
        if (node.getType().isVar()) {
            ITypeBinding binding = node.getType().resolveBinding();
            if (binding == null) {
                System.out.println("null type " + node);
            }
            else {
                node.setType(makeType(binding, node.getAST()));
            }
        }
        return super.visit(node);
    }

    Type makeType(ITypeBinding binding, AST ast) {
        if (binding.isArray()) {
            return ast.newArrayType(makeType(binding.getElementType(), ast), binding.getDimensions());
        }
        else {
            return ast.newSimpleType(ast.newSimpleName(binding.getName()));
        }
    }
}
