package se.lth.cs.tycho.phases.cbackend;

import org.multij.Binding;
import org.multij.BindingKind;
import org.multij.Module;
import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.types.ListType;
import se.lth.cs.tycho.types.Type;

import java.util.stream.Stream;

@Module
public interface Lists {
    @Binding(BindingKind.MODULE)
    Backend backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default void declareListTypes() {
        listTypes().forEachOrdered(this::declareType);
    }

    default void declareType(ListType type) {
        String typeName = backend().code().type(type);
        String elementType = backend().code().type(type.getElementType());
        int size = type.getSize().getAsInt();

        emitter().emit("typedef struct {");
        emitter().increaseIndentation();
        emitter().emit("%s data[%d];", elementType, size);
        emitter().decreaseIndentation();
        emitter().emit("} %s;", typeName);
    }

    default Stream<ListType> listTypes() {
        return backend().task().walk()
                .flatMap(this::listType)
                .distinct();
    }

    default Stream<ListType> listType(IRNode node) {
        return Stream.empty();
    }

    default Stream<ListType> listType(VarDecl decl) {
        return wrapIfList(backend().types().declaredType(decl));
    }
    default Stream<ListType> listType(Expression expr) {
        Type t = backend().types().type(expr);
        return wrapIfList(t);
    }

    default Stream<ListType> wrapIfList(Type t) {
        return Stream.empty();
    }

    default Stream<ListType> wrapIfList(ListType t) {
        return Stream.of(t);
    }
}
