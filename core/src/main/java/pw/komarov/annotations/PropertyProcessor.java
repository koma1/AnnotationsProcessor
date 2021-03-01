package pw.komarov.annotations;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.Random;
import java.util.Set;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

@SupportedAnnotationTypes({"pw.komarov.annotations.Property"})
public class PropertyProcessor extends AbstractProcessor {

    private TreeMaker treeMaker;
    private Messager messager;
    private JavacElements elementUtils;

    @Override
    public void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        this.treeMaker = TreeMaker.instance(((JavacProcessingEnvironment) processingEnvironment).getContext());
        this.elementUtils = ((JavacProcessingEnvironment) processingEnvironment).getElementUtils();
        this.messager = processingEnvironment.getMessager();
    }

    private String generateMethodName(String prefix, String varName) {
        return prefix + Character.toUpperCase(varName.charAt(0)) + varName.substring(1);
    }

    private JCTree.JCMethodDecl generateGetter(String fieldName, Type type, boolean isStatic) {
        String methodName = generateMethodName("get", fieldName); //имя будущего геттера
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC | (isStatic ? Flags.STATIC : 0)); //флаги будущего геттера

        JCTree.JCBlock body = treeMaker.Block(0, List.of( //код будущего геттера
                isStatic ?
                        treeMaker.Return(treeMaker.Ident(elementUtils.getName(fieldName))) //for static - without "this"
                        : treeMaker.Return(treeMaker.Select(treeMaker.This(Type.noType), elementUtils.getName(fieldName))) //for non-static - with "this"
        ));

        return treeMaker.MethodDef( //объявление метода будущего геттера
                modifiers,
                elementUtils.getName(methodName),
                treeMaker.Type(type),
                List.nil(),
                List.nil(),
                List.nil(),
                body,
                null
        );
    }

    private JCTree.JCMethodDecl generateSetter(String fieldName, Type type, boolean isStatic) {
        String methodName = generateMethodName("set", fieldName);
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC | (isStatic ? Flags.STATIC : 0));

        Name paramName = elementUtils.getName("value" + Math.abs(new Random().nextLong())); //сгенерируем рандомное имя параметра (чтобы случайно не имело единого имени с полем)

        JCTree.JCExpression binary = treeMaker.Assign(
                isStatic ?
                        treeMaker.Ident(elementUtils.getName(fieldName)) //имя поля без "this" (т.к. оно static)
                        : treeMaker.Select(treeMaker.This(Type.noType), elementUtils.getName(fieldName)), //если поле не static - используем "this"
                treeMaker.Ident(paramName)
        );

        JCTree.JCBlock body = treeMaker.Block(0, List.of(
                treeMaker.Exec(binary)
        ));

        return treeMaker.MethodDef(
                modifiers,
                elementUtils.getName(methodName),
                treeMaker.Type(new Type.JCVoidType()),
                List.nil(),
                List.of(treeMaker.Param(paramName, type, null)),
                List.nil(),
                body,
                null
        );
    }

    private boolean existsMethod(String methodName, Element rootElement) {
        for(Element element : rootElement.getEnclosedElements())
            if((element.getKind() == ElementKind.METHOD) && (element.getSimpleName().toString().equals(methodName)))
                return true;

        return false;
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        for (Element rootElement : roundEnvironment.getRootElements()) {
            JCTree rootTree = elementUtils.getTree(rootElement);
            for (Element element : rootElement.getEnclosedElements()) {

//                messager.printMessage(Diagnostic.Kind.NOTE,
//                        "for element: " + element.getSimpleName() + " kind: " + element.getKind().toString()
//                                + " class: " + element.getClass().toString());

                if (element.getKind() == ElementKind.FIELD) {
                    Property annotation = element.getAnnotation(Property.class);
                    if (annotation != null) {
                        if(!existsMethod(generateMethodName("get", element.toString()), element)) //проверить нет ли существующего геттера и если он есть - не генерировать
                            ((JCClassDecl) rootTree).defs = ((JCClassDecl) rootTree).defs.append(
                                    generateGetter(
                                            element.toString(),
                                            ((Symbol.VarSymbol) element).type,
                                            element.getModifiers().contains(Modifier.STATIC)
                                    )
                            );

                        if
                        ((!element.getModifiers().contains(Modifier.FINAL)) //если поле final, то сеттера там быть не может
                                &&
                                ((!existsMethod(generateMethodName("set", element.toString()), element)))) //проверить нет ли существующего сеттера и если он есть - не генерировать
                            ((JCClassDecl) rootTree).defs = ((JCClassDecl) rootTree).defs.append(
                                    generateSetter(
                                            element.toString(),
                                            ((Symbol.VarSymbol) element).type,
                                            element.getModifiers().contains(Modifier.STATIC)
                                    )
                            );
                    }
                }
            }

//            messager.printMessage(Diagnostic.Kind.NOTE,
//                    "\t source:" +
//                            rootTree.toString()
//
//            );

            return false;
        }

        return true;
    }
}