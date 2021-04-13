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
        else {
            for (Object f : node.fragments()) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
                if (fragment.getInitializer() instanceof LambdaExpression) {
                    LambdaExpression lambda = (LambdaExpression) fragment.getInitializer();
                    fragment.setInitializer(lambda(lambda));
                }
                else if (fragment.getInitializer() instanceof ExpressionMethodReference) {
                    ExpressionMethodReference ref = (ExpressionMethodReference) fragment.getInitializer();
                    Type type = makeType(ref.resolveTypeBinding(), node.getAST(), true);
                    IMethodBinding func = ref.resolveTypeBinding().getFunctionalInterfaceMethod();
                    fragment.setInitializer(makeAnony(type, func, makeBody(ref.resolveMethodBinding(), node.getAST()), node.getAST()));
                }
                else if (fragment.getInitializer() instanceof CreationReference) {
                    CreationReference reference = (CreationReference) fragment.getInitializer();
                    fragment.setInitializer(makeRefCons(reference));
                }
            }
        }
        return super.visit(node);
    }

    Expression makeRefCons(CreationReference reference) {
        AST ast = reference.getAST();
        Type type = reference.getType();
        type = makeType(type.resolveBinding(), ast, true);
        //make body
        Block block = ast.newBlock();
        if (type.isArrayType()) {
            return makeAnony(type, reference.resolveTypeBinding().getFunctionalInterfaceMethod(), makeBody(reference.resolveMethodBinding(), ast), ast);
        }
        else {
            return makeAnony(type, type.resolveBinding().getFunctionalInterfaceMethod(), makeBody(reference.resolveMethodBinding(), ast), ast);
        }
    }

    Expression makeAnony(Type base, IMethodBinding func, Block body, AST ast) {
        ClassInstanceCreation creation = ast.newClassInstanceCreation();
        creation.setType(base);
        AnonymousClassDeclaration an = ast.newAnonymousClassDeclaration();
        an.bodyDeclarations().add(makeMethod(func, body, ast));
        creation.setAnonymousClassDeclaration(an);
        return creation;
    }

    Block makeBody(IMethodBinding ref, AST ast) {
        Block block = ast.newBlock();
        MethodInvocation call = ast.newMethodInvocation();
        call.setName(ast.newSimpleName(ref.getName()));
        for (int i = 0; i < ref.getParameterTypes().length; i++) {
            call.arguments().add(ast.newSimpleName("p" + i));
        }

        if (ref.isConstructor()) {
            ReturnStatement returnStatement = ast.newReturnStatement();
            ClassInstanceCreation ins = ast.newClassInstanceCreation();
            ins.setType(makeType(ref.getDeclaringClass(), ast, false));
            for (ITypeBinding arg : ref.getParameterTypes()) {
                //ins.arguments();
            }
            returnStatement.setExpression(ins);
            block.statements().add(returnStatement);
        }
        else {
            if (ref.getReturnType().isPrimitive() && ref.getReturnType().getName().equals("void")) {
                block.statements().add(ast.newExpressionStatement(call));
            }
            else {
                ReturnStatement returnStatement = ast.newReturnStatement();
                returnStatement.setExpression(call);
                block.statements().add(returnStatement);
            }
        }
        return block;
    }

    Block copy(Block block) {
        Block res = block.getAST().newBlock();
        res.statements().addAll(ASTNode.copySubtrees(block.getAST(), block.statements()));
        return res;
    }

    MethodDeclaration makeMethod(IMethodBinding binding, Block body, AST ast) {
        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setReturnType2(makeType(binding.getReturnType(), ast, true));
        methodDeclaration.setName(ast.newSimpleName(binding.getName()));
        if (Modifier.isPublic(binding.getModifiers())) {
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        }
        if (Modifier.isStatic(binding.getModifiers())) {
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
        }
        int i = 0;
        for (ITypeBinding arg : binding.getParameterTypes()) {
            SingleVariableDeclaration v = ast.newSingleVariableDeclaration();
            v.setType(makeType(arg, ast, true));
            v.setName(ast.newSimpleName("p" + i++));
            methodDeclaration.parameters().add(v);
        }
        methodDeclaration.setBody(copy(body));
        return methodDeclaration;
    }

    Expression lambda(LambdaExpression lambda) {
        AST ast = lambda.getAST();
        IMethodBinding binding = lambda.resolveMethodBinding();
        Type type = makeType(binding.getDeclaringClass(), ast, false);
        Block body;
        if (lambda.getBody() instanceof Block) {
            body = (Block) lambda.getBody();
        }
        else {
            body = ast.newBlock();
            ASTNode node = ASTNode.copySubtree(lambda.getAST(), lambda.getBody());
            body.statements().add(ast.newExpressionStatement((Expression) node));
        }
        return makeAnony(type, binding, body, ast);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        for (ListIterator it = node.arguments().listIterator(); it.hasNext(); ) {
            Object arg = it.next();
            if (arg instanceof ExpressionMethodReference) {
                ExpressionMethodReference ref = (ExpressionMethodReference) arg;
                Type type = makeType(ref.resolveTypeBinding(), node.getAST(), true);
                IMethodBinding func = ref.resolveTypeBinding().getFunctionalInterfaceMethod();
                it.set(makeAnony(type, func, makeBody(ref.resolveMethodBinding(), node.getAST()), node.getAST()));
            }
            else if (arg instanceof LambdaExpression) {
                LambdaExpression l = (LambdaExpression) arg;
                it.set(lambda(l));
            }
            else if (arg instanceof CreationReference) {
                CreationReference reference = (CreationReference) arg;
                it.set(makeRefCons(reference));
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
