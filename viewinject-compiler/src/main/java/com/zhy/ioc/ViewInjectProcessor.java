package com.zhy.ioc;

import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Created by zhy on 16/4/22.
 * 注解处理器一般继承于AbstractProcessor,部分代码的写法基本是固定的
 */
@AutoService(Processor.class)
public class ViewInjectProcessor extends AbstractProcessor {

    // 跟日志相关的辅助类
    private Messager messager;
    // 跟元素相关的辅助类，帮助我们去获取一些元素相关的信息。
    private Elements elementUtils;
    // 用来存放收集到的信息
    private Map<String, ProxyInfo> mProxyMap = new HashMap<String, ProxyInfo>();

    /**
     * 复写
     * @param processingEnv 可以帮助我们去初始化一些父类类
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
    }

    /**
     * 返回支持的注解类型
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(Bind.class.getCanonicalName());
        return supportTypes;
    }

    /**
     * 返回支持的源码版本
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 两个大步骤:
     * 一：收集信息  -  根据你的注解声明，拿到对应的Element，然后获取到我们所需要的信息，这个信息肯定是为了后面生成JavaFileObject所准备的。
     * 二：生成代理类（本文把编译时生成的类叫代理类）
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "process...");
        // 因为process可能会多次调用，避免生成重复的代理类，避免生成类的类名已存在异常。
        mProxyMap.clear();

        // 拿到我们通过@Bind注解的元素
        Set<? extends Element> elesWithBind = roundEnv.getElementsAnnotatedWith(Bind.class);
        //一、收集信息
        for (Element element : elesWithBind) {
            //检查element类型
            checkAnnotationValid(element, Bind.class);

            VariableElement variableElement = (VariableElement) element;
            //class type
            TypeElement classElement = (TypeElement) variableElement.getEnclosingElement();
            //full class name
            String fqClassName = classElement.getQualifiedName().toString();

            // 生成ProxyInfo对象
            // 这里通过一个mProxyMap进行检查，key为qualifiedName即类的全路径，如果没有生成才会去生成一个新的，ProxyInfo与类是一一对应的。
            ProxyInfo proxyInfo = mProxyMap.get(fqClassName);
            if (proxyInfo == null) {
                proxyInfo = new ProxyInfo(elementUtils, classElement);
                mProxyMap.put(fqClassName, proxyInfo);
            }

            Bind bindAnnotation = variableElement.getAnnotation(Bind.class);
            int id = bindAnnotation.value();
            // 会将与该类对应的且被@BindView声明的VariableElement加入到ProxyInfo中去，key为我们声明时填写的id，即View的id。
            proxyInfo.injectVariables.put(id, variableElement);
        }

        // b.生成代理类
        // 主要就是遍历我们的mProxyMap，然后取得每一个ProxyInfo，最后通过mFileUtils.createSourceFile来创建文件对象，
        // 类名为proxyInfo.getProxyClassFullName()，写入的内容为proxyInfo.generateJavaCode().
        for (String key : mProxyMap.keySet()) {
            ProxyInfo proxyInfo = mProxyMap.get(key);
            try {
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                        proxyInfo.getProxyClassFullName(),
                        proxyInfo.getTypeElement());
                Writer writer = jfo.openWriter();
                writer.write(proxyInfo.generateJavaCode());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(proxyInfo.getTypeElement(),
                        "Unable to write injector for type %s: %s",
                        proxyInfo.getTypeElement(), e.getMessage());
            }

        }
        return true;
    }

    private boolean checkAnnotationValid(Element annotatedElement, Class clazz) {
        if (annotatedElement.getKind() != ElementKind.FIELD) {
            error(annotatedElement, "%s must be declared on field.", clazz.getSimpleName());
            return false;
        }
        if (ClassValidator.isPrivate(annotatedElement)) {
            error(annotatedElement, "%s() must can not be private.", annotatedElement.getSimpleName());
            return false;
        }

        return true;
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
    }
}
