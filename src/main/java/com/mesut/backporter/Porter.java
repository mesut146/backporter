package com.mesut.backporter;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Porter {
    File src;
    File dest;
    ASTParser parser;
    List<String> classpath = new ArrayList<>();
    List<String> sourceList;

    public Porter(File src, File dest) {
        this.src = src;
        this.dest = dest;
    }

    //jar or dir
    public void addClasspath(String path) {
        classpath.add(path);
    }

    @SuppressWarnings("rawtypes,unchecked")
    public void initParser() {
        parser = ASTParser.newParser(AST.getJLSLatest());
        var cpDirs = new ArrayList<String>();
        var cpJars = new ArrayList<String>();

        for (var path : classpath) {
            if (path.endsWith(".jar")) {
                cpJars.add(path);
            }
            else {
                cpDirs.add(path);
            }
        }
        //cpDirs.add(src.getPath());
        parser.setEnvironment(cpJars.toArray(new String[0]), cpDirs.toArray(new String[0]), null, true);

        parser.setResolveBindings(true);
        //parser.setBindingsRecovery(true);
        //parser.setStatementsRecovery(true);

        var options = JavaCore.getOptions();
        var ver = JavaCore.latestSupportedJavaVersion();
        //options.put(JavaCore.COMPILER_COMPLIANCE, ver);
        options.put(JavaCore.COMPILER_SOURCE, ver);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, ver);
        //options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES,"true");
        parser.setCompilerOptions(options);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
    }

    void collect(File dir) {
        if (dir.isDirectory()) {
            for (var file : dir.listFiles()) {
                if (file.isDirectory()) {
                    collect(file);
                }
                else if (file.getName().endsWith(".java")) {
                    sourceList.add(file.getAbsolutePath());
                }
            }
        }
    }

    public void port() {
        sourceList = new ArrayList<>();
        collect(src);
        System.out.println("total of " + sourceList.size() + " files");
        initParser();
        String[] b = new String[sourceList.size()];
        Arrays.fill(b, "");
        parser.createASTs(sourceList.toArray(new String[0]), new String[sourceList.size()], b, new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                single(ast, sourceFilePath);
            }
        }, null);
    }

    private void single(CompilationUnit ast, String path) {
        System.out.println("porting " + path);
        MyVisitor visitor = new MyVisitor(ast);
        ast.accept(visitor);
        ast.accept(new MethodPorter());
        String relPath = Util.trimPrefix(path, src.getAbsolutePath());
        Path target = Paths.get(dest.getAbsolutePath(), relPath);

        try {
            target.toFile().getParentFile().mkdirs();
            target.toFile().createNewFile();
            CodeFormatter formatter = ToolFactory.createCodeFormatter(null);
            String code = ast.toString();
            TextEdit edit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, code, 0, code.length(), 0, null);
            IDocument doc = new Document(code);
            edit.apply(doc);
            var data = doc.get();
            //data = NioHelper.replace(data);
            Files.write(target, data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
