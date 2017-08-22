package com.airbnb.paris.processor

import com.airbnb.paris.annotations.NewStyle
import com.airbnb.paris.processor.utils.Errors
import com.airbnb.paris.processor.utils.ProcessorException
import com.airbnb.paris.processor.utils.check
import com.airbnb.paris.processor.utils.fail
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal class NewStyleInfo private constructor(
        val elementKind: Kind,
        val enclosingElement: Element,
        val targetType: TypeMirror,
        val elementName: String) {

    companion object {

        lateinit var elementUtils: Elements
        lateinit var typeUtils: Types

        fun fromEnvironment(roundEnv: RoundEnvironment, elementUtils: Elements, typeUtils: Types): List<NewStyleInfo> {
            this.elementUtils = elementUtils
            this.typeUtils = typeUtils

            return roundEnv.getElementsAnnotatedWith(NewStyle::class.java)
                    .mapNotNull {
                        try {
                            fromElement(it)
                        } catch (e: ProcessorException) {
                            Errors.log(e)
                            null
                        }
                    }
        }

        @Throws(ProcessorException::class)
        private fun fromElement(element: Element): NewStyleInfo {
            check(element.modifiers.contains(Modifier.STATIC)
                    && !element.modifiers.contains(Modifier.PRIVATE)
                    && !element.modifiers.contains(Modifier.PROTECTED), element) {
                "Field and methods annotated with @NewStyle must be static and can't be private or protected"
            }

            val enclosingElement = element.enclosingElement

            var elementKind: Kind? = null
            var targetType: TypeMirror? = null
            if (element.kind == ElementKind.FIELD) {
                elementKind = Kind.FIELD
                targetType = element.asType()

                // TODO Check that the target type is an int
            } else if (element.kind == ElementKind.METHOD) {
                elementKind = Kind.METHOD
                targetType = (element as ExecutableElement).parameters[0].asType()

                // TODO Check that the target type is a builder
            } else {
                fail(element) {
                    "@NewStyle can only be used on fields and methods"
                }
            }

            val elementName = element.simpleName.toString()

            return NewStyleInfo(elementKind!!, enclosingElement, targetType!!, elementName)
        }
    }

    enum class Kind {
        FIELD, METHOD
    }
}