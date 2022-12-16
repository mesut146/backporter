package com.mesut.backporter;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class MyVisitor extends ASTVisitor {

    public static boolean ref = true;
    CompilationUnit unit;

    public MyVisitor(CompilationUnit ast) {
        this.unit = ast;

    }

    @Override
    public boolean visit(MethodInvocation node) {
        for (var it = node.arguments().listIterator(); it.hasNext(); ) {
            var arg = it.next();
            if (arg instanceof ExpressionMethodReference && ref) {
                var ref = (ExpressionMethodReference) arg;
                var type = makeType(ref.resolveTypeBinding(), node.getAST(), true);
                var func = ref.resolveTypeBinding().getFunctionalInterfaceMethod();
                it.set(makeAnony(type, func, makeBody(ref.resolveMethodBinding(), node.getAST()), node.getAST()));
            }
            else if (arg instanceof LambdaExpression) {
                var l = (LambdaExpression) arg;
                it.set(lambda(l));
            }
            else if (arg instanceof CreationReference && ref) {
                var reference = (CreationReference) arg;
                it.set(makeRefCons(reference));

            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        if (node.getType().isVar()) {
            node.setType(handleVar(node.getType()));
        }
        else {
            for (Object f : node.fragments()) {
                var fragment = (VariableDeclarationFragment) f;
                handleFrag(fragment);
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {
        if (node.getType().isVar()) {
            node.setType(handleVar(node.getType()));
        }
        for (Object f : node.fragments()) {
            var fragment = (VariableDeclarationFragment) f;
            handleFrag(fragment);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        if (node.getType().isVar()) {
            node.setType(handleVar(node.getType()));
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedType node) {
        return super.visit(node);
    }

    Type handleVar(Type type) {
        var binding = type.resolveBinding();
        if (binding == null) {
            System.out.println("null var type = " + type.getParent());
            return (Type) ASTNode.copySubtree(type.getAST(), type);
        }
        else {
            addImport(binding);
            return makeType(binding, type.getAST(), false);
        }
    }

    void addImport(ITypeBinding binding) {
        if (binding.isArray()) {
            binding = binding.getElementType();
        }
        if (binding.isPrimitive() || binding.isLocal()) return;
        for (Object o : unit.imports()) {
            var importDeclaration = (ImportDeclaration) o;
            var b = importDeclaration.resolveBinding();
            if (b != null) {
                if (b.isEqualTo(binding)) {
                    return;
                }
            }
        }
        var declaration = unit.getAST().newImportDeclaration();
        var name = binding.getBinaryName();
        if (name.contains("$")) {
            name = name.replace("$", ".");
        }
        declaration.setName(unit.getAST().newName(name));
        unit.imports().add(declaration);
    }

    void handleFrag(VariableDeclarationFragment fragment) {
        var ast = fragment.getAST();
        if (fragment.getInitializer() instanceof LambdaExpression) {
            var lambda = (LambdaExpression) fragment.getInitializer();
            fragment.setInitializer(lambda(lambda));
        }
        else if (fragment.getInitializer() instanceof ExpressionMethodReference && ref) {
            var ref = (ExpressionMethodReference) fragment.getInitializer();
            var type = makeType(ref.resolveTypeBinding(), ast, true);
            var func = ref.resolveTypeBinding().getFunctionalInterfaceMethod();
            fragment.setInitializer(makeAnony(type, func, makeBody(ref.resolveMethodBinding(), ast), ast));
        }
        else if (fragment.getInitializer() instanceof CreationReference && ref) {
            var reference = (CreationReference) fragment.getInitializer();
            fragment.setInitializer(makeRefCons(reference));
        }
    }

    Expression makeRefCons(CreationReference reference) {
        var ast = reference.getAST();
        var binding = reference.resolveTypeBinding();
        //Type type = makeType(reference.getType().resolveBinding(), ast, true);
        var type = makeType(binding, ast, true);
        //make body
        var block = ast.newBlock();
        if (reference.getType().isArrayType()) {
            var arrayType = (ArrayType) reference.getType();
            var elemType = arrayType.getElementType();
            var ret = ast.newReturnStatement();
            var arrayCreation = ast.newArrayCreation();
            ASTNode.copySubtree(reference.getAST(), arrayType);
            arrayCreation.setType(ast.newArrayType((Type) ASTNode.copySubtree(reference.getAST(), elemType), arrayType.getDimensions()));
            arrayCreation.dimensions().add(ast.newSimpleName("p0"));
            ret.setExpression(arrayCreation);
            block.statements().add(ret);
            return makeAnony(type, binding.getFunctionalInterfaceMethod(), block, ast);
        }
        else {
            block = makeBody(reference.resolveMethodBinding(), ast);
            return makeAnony(type, binding.getFunctionalInterfaceMethod(), block, ast);
        }
    }

    Expression makeAnony(Type base, IMethodBinding func, Block body, AST ast) {
        return makeAnony(base, func, body, null, ast);
    }

    Expression makeAnony(Type base, IMethodBinding func, Block body, List<String> params, AST ast) {
        var creation = ast.newClassInstanceCreation();
        creation.setType(base);
        var an = ast.newAnonymousClassDeclaration();
        an.bodyDeclarations().add(makeMethod(func, body, params, ast));
        creation.setAnonymousClassDeclaration(an);
        return creation;
    }

    Block makeBody(IMethodBinding ref, AST ast) {
        var block = ast.newBlock();

        if (ref.isConstructor()) {
            var returnStatement = ast.newReturnStatement();
            var ins = ast.newClassInstanceCreation();
            ins.setType(makeType(ref.getDeclaringClass(), ast, false));
            for (int j = 0; j < ref.getParameterTypes().length; j++) {
                ins.arguments().add(ast.newSimpleName("p" + j));
            }
            returnStatement.setExpression(ins);
            block.statements().add(returnStatement);
        }
        else {
            var call = ast.newMethodInvocation();
            call.setName(ast.newSimpleName(ref.getName()));
            for (int i = 0; i < ref.getParameterTypes().length; i++) {
                call.arguments().add(ast.newSimpleName("p" + i));
            }
            if (ref.getReturnType().getName().equals("void")) {
                block.statements().add(ast.newExpressionStatement(call));
            }
            else {
                var returnStatement = ast.newReturnStatement();
                returnStatement.setExpression(call);
                block.statements().add(returnStatement);
            }
        }
        return block;
    }

    Block copy(Block block) {
        var res = block.getAST().newBlock();
        res.statements().addAll(ASTNode.copySubtrees(block.getAST(), block.statements()));
        return res;
    }

    MethodDeclaration makeMethod(IMethodBinding binding, Block body, List<String> params, AST ast) {
        var methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setReturnType2(makeType(binding.getReturnType(), ast, true));
        methodDeclaration.setName(ast.newSimpleName(binding.getName()));
        if (Modifier.isPublic(binding.getModifiers())) {
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        }
        if (Modifier.isStatic(binding.getModifiers())) {
            methodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
        }
        int i = 0;
        for (var arg : binding.getParameterTypes()) {
            var v = ast.newSingleVariableDeclaration();
            v.setType(makeType(arg, ast, true));
            if (params == null) {
                v.setName(ast.newSimpleName("p" + i++));
            }
            else {
                v.setName(ast.newSimpleName(params.get(i)));
            }
            methodDeclaration.parameters().add(v);
        }
        methodDeclaration.setBody(copy(body));
        return methodDeclaration;
    }

    Expression lambda(LambdaExpression lambda) {
        var ast = lambda.getAST();
        var binding = lambda.resolveMethodBinding();
        addImport(binding.getDeclaringClass());
        var type = makeType(binding.getDeclaringClass(), ast, false);
        Block body;
        if (lambda.getBody() instanceof Block) {
            body = (Block) lambda.getBody();
        }
        else {
            body = ast.newBlock();
            var node = (Expression) ASTNode.copySubtree(lambda.getAST(), lambda.getBody());
            var ret = ast.newReturnStatement();
            ret.setExpression(node);
            body.statements().add(ret);
        }
        var params = new ArrayList<String>();
        for (var o : lambda.parameters()) {
            if (o instanceof SingleVariableDeclaration) {
                params.add(((SingleVariableDeclaration) o).getName().getIdentifier());
            }
            else {
                params.add(((VariableDeclarationFragment) o).getName().getIdentifier());
            }
        }
        return makeAnony(type, binding, body, params, ast);
    }

    Type makeType(ITypeBinding binding, AST ast, boolean infer) {
        if (binding.isArray()) {
            return ast.newArrayType(makeType(binding.getElementType(), ast, false), binding.getDimensions());
        }
        else if (binding.isPrimitive()) {
            return ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
        }
        else if (binding.isParameterizedType()) {
            var type = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(binding.getErasure().getName())));
            for (ITypeBinding arg : binding.getTypeArguments()) {
                type.typeArguments().add(makeType(arg, ast, infer));
            }
            return type;
        }
        else if (binding.isWildcardType()) {
            var type = ast.newWildcardType();
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
