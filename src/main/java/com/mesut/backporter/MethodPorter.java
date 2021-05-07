package com.mesut.backporter;

import org.eclipse.jdt.core.dom.*;

public class MethodPorter extends ASTVisitor {

    Name getUtil(AST ast) {
        return ast.newName("utils.Util");
    }

    @Override
    public boolean visit(MethodInvocation node) {
        AST ast = node.getAST();
        if (node.getExpression() != null) {
            ITypeBinding type = node.getExpression().resolveTypeBinding();
            if (type != null) {
                if (type.isCapture()) {
                    type = type.getErasure();
                }
                if (type.getBinaryName().equals("java.util.List")) {
                    if (node.getName().getIdentifier().equals("of")) {
                        node.setName(ast.newSimpleName("listOf"));
                        node.setExpression(getUtil(ast));
                    }
                }
                else if (type.getBinaryName().equals("java.util.Set")) {
                    if (node.getName().getIdentifier().equals("of")) {
                        node.setName(ast.newSimpleName("setOf"));
                        node.setExpression(getUtil(ast));
                    }
                }
                else if (type.getBinaryName().equals("java.util.Map")) {
                    if (node.getName().getIdentifier().equals("of")) {
                        node.setName(ast.newSimpleName("mapOf"));
                        node.setExpression(getUtil(ast));
                    }
                }
                else if (type.getBinaryName().equals("java.io.File")) {
                    if (node.getName().getIdentifier().equals("toPath")) {
                        node.setName(ast.newSimpleName("toPath"));
                        node.arguments().add(ASTNode.copySubtree(ast, node.getExpression()));
                        node.setExpression(getUtil(ast));
                    }
                }
            }

        }
        return super.visit(node);
    }
}
