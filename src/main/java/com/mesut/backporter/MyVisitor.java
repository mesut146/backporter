package com.mesut.backporter;

import org.eclipse.jdt.core.dom.*;

import java.util.ListIterator;

public class MyVisitor extends ASTVisitor {

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        if (node.getType().isVar()) {
            ITypeBinding binding = node.getType().resolveBinding();
            if (binding == null) {
                System.out.println("null type " + node);
            }
            else {
                node.setType(makeType(binding, node.getAST(), false));
            }
        }
        return super.visit(node);
    }

    Expression makeAnony(Type base, IMethodBinding func, IMethodBinding ref, AST ast) {
        ClassInstanceCreation creation = ast.newClassInstanceCreation();
        creation.setType(base);
        AnonymousClassDeclaration an = ast.newAnonymousClassDeclaration();
        an.bodyDeclarations().add(makeMethod(func, ref, ast));
        creation.setAnonymousClassDeclaration(an);
        return creation;
    }

    MethodDeclaration makeMethod(IMethodBinding binding, IMethodBinding methodBinding, AST ast) {
        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setReturnType2(makeType(binding.getReturnType(), ast, true));
        methodDeclaration.setName(ast.newSimpleName(binding.getName()));
        if (Modifier.isPublic(binding.getModifiers())) {
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        }
        if (Modifier.isStatic(binding.getModifiers())) {
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
        }
        for (ITypeBinding arg : binding.getParameterTypes()) {
            SingleVariableDeclaration v = ast.newSingleVariableDeclaration();
            v.setType(makeType(arg, ast, true));
            v.setName(ast.newSimpleName("p0"));
            methodDeclaration.parameters().add(v);
        }
        Block block = ast.newBlock();
        ReturnStatement returnStatement = ast.newReturnStatement();
        MethodInvocation i = ast.newMethodInvocation();
        i.setName(ast.newSimpleName(methodBinding.getName()));
        i.arguments().add(ast.newSimpleName("p0"));
        returnStatement.setExpression(i);
        block.statements().add(returnStatement);
        methodDeclaration.setBody(block);
        return methodDeclaration;
    }

    @Override
    public boolean visit(MethodInvocation node) {
        for (ListIterator it = node.arguments().listIterator(); it.hasNext(); ) {
            Object arg = it.next();
            if (arg instanceof ExpressionMethodReference) {
                ExpressionMethodReference ref = (ExpressionMethodReference) arg;
                Type type = makeType(ref.resolveTypeBinding(), node.getAST(), true);
                IMethodBinding func = ref.resolveTypeBinding().getFunctionalInterfaceMethod();
                it.set(makeAnony(type, func, ref.resolveMethodBinding(), node.getAST()));
            }
            else if (arg instanceof LambdaExpression) {
                LambdaExpression l = (LambdaExpression) arg;
                Type type = makeType(l.resolveTypeBinding(), node.getAST(), true);
                it.set(makeAnony(type, l.resolveTypeBinding().getFunctionalInterfaceMethod(), l.resolveMethodBinding(), node.getAST()));
            }
            else if (arg instanceof CreationReference) {
                CreationReference reference = (CreationReference) arg;
                Type type = reference.getType();
                type = makeType(type.resolveBinding(), node.getAST(), true);
                if (type.isArrayType()) {
                    it.set(makeAnony(type, reference.resolveTypeBinding().getFunctionalInterfaceMethod(), reference.resolveMethodBinding(), node.getAST()));
                }
                else {
                    it.set(makeAnony(type, type.resolveBinding().getFunctionalInterfaceMethod(), reference.resolveMethodBinding(), node.getAST()));
                }
                /*List<String> list = null;
                String[] s = list.toArray(String[]::new);
                list.toArray(new IntFunction<String[]>() {
                    @Override
                    public String[] apply(int i) {
                        return new String[0];
                    }
                });*/
            }
        }
        return super.visit(node);
    }

    Type makeType(ITypeBinding binding, AST ast, boolean infer) {
        if (binding.isArray()) {
            return ast.newArrayType(makeType(binding.getElementType(), ast, false), binding.getDimensions());
        }
        else if (binding.isPrimitive()) {
            return ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
        }
        else if (binding.isParameterizedType()) {
            ParameterizedType type = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(binding.getErasure().getName())));
            for (ITypeBinding arg : binding.getTypeArguments()) {
                type.typeArguments().add(makeType(arg, ast, infer));
            }
            return type;
        }
        else if (binding.isWildcardType()) {
            WildcardType type = ast.newWildcardType();
            if (binding.getBound() != null) {
                if (infer) {
                    return makeType(binding.getBound(), ast, false);
                }
                type.setBound(makeType(binding.getBound(), ast, false), binding.isUpperbound());
            }
            return type;
        }
        else if (binding.isCapture()) {
            return makeType(binding.getWildcard(), ast, infer);
        }
        else {
            return ast.newSimpleType(ast.newSimpleName(binding.getName()));
        }
    }
}
