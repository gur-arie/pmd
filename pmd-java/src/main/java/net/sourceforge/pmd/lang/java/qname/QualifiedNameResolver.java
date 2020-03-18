/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.qname;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

import net.sourceforge.pmd.annotation.InternalApi;
import net.sourceforge.pmd.lang.java.ast.ASTAnonymousClassDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTAnyTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFormalParameter;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodOrConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.ast.InternalApiBridge;
import net.sourceforge.pmd.lang.java.ast.JavaParserVisitorAdapter;
import net.sourceforge.pmd.lang.java.symbols.JClassSymbol;
import net.sourceforge.pmd.lang.java.symbols.JExecutableSymbol;
import net.sourceforge.pmd.lang.java.symbols.JTypeParameterOwnerSymbol;
import net.sourceforge.pmd.lang.java.symbols.internal.impl.ast.AstSymFactory;


/**
 *
 * <p>In fact, populates symbols on declaration nodes.
 * TODO in the near future we'll get rid of qualified names, and can
 * reuse this class just to build symbols (moving it to symbols.impl.ast).
 *
 * @author Clément Fournier
 * @since 6.1.0
 * @deprecated Is internal API
 */
@Deprecated
@InternalApi
public class QualifiedNameResolver extends JavaParserVisitorAdapter {

    /** Local index value for when the class is not local. */
    static final int NOTLOCAL_PLACEHOLDER = -1;

    // The following stacks stack some counter of the
    // visited classes. A new entry is pushed when
    // we enter a new class, and popped when we get
    // out

    /**
     * The top of the stack is the map of local class names
     * to the count of local classes with that name declared
     * in the current class.
     */
    private final Deque<Map<String, Integer>> currentLocalIndices = new ArrayDeque<>();

    /**
     * The top of the stack is the current count of
     * anonymous classes of the currently visited class.
     */
    private final Deque<MutableInt> anonymousCounters = new ArrayDeque<>();


    // these two are 1-to-1
    private final Deque<String> innermostEnclosingTypeName = new ArrayDeque<>();
    private final Deque<JTypeParameterOwnerSymbol> enclosingSymbols = new ArrayDeque<>();

    private final AstSymFactory symFactory;

    /** Package name of the current file. */
    private String packageName;

    public QualifiedNameResolver(AstSymFactory symFactory) {
        this.symFactory = symFactory;
    }

    /**
     * Traverse the compilation unit.
     */
    public void traverse(ASTCompilationUnit root) {
        root.jjtAccept(this, null);
    }


    @Override
    public Object visit(ASTCompilationUnit node, Object data) {

        // update the package list
        packageName = node.getPackageName();

        // reset other variables
        anonymousCounters.clear();
        currentLocalIndices.clear();

        return super.visit(node, data);
    }


    @Override
    public Object visit(ASTVariableDeclaratorId node, Object data) {

        if (isTrueLocalVar(node)) {
            symFactory.setLocalVarSymbol(node);
        } else {
            // in the other cases, building the method/ctor/class symbols already set the symbols
            assert node.getSymbol() != null : "Symbol was null for " + node;
        }

        return super.visit(node, data);
    }

    private boolean isTrueLocalVar(ASTVariableDeclaratorId node) {
        return !(node.isField() || node.isEnumConstant() || node.getParent() instanceof ASTFormalParameter);
    }


    @Override
    public Object visit(ASTAnyTypeDeclaration node, Object data) {
        String simpleName = node.getSimpleName();
        if (node.isLocal()) {
            simpleName = getNextIndexFromHistogram(currentLocalIndices.getFirst(), node.getSimpleName(), 1)
                + simpleName;
        }

        updateClassContext(simpleName);

        return recurseOnClass(node);
    }

    @Override
    public Object visit(ASTAnonymousClassDeclaration node, Object data) {
        updateContextForAnonymousClass();
        return recurseOnClass(node);
    }


    @Override
    public Object visit(ASTMethodDeclaration node, Object data) {
        return recurseOnExecutable(node);
    }

    @Override
    public Object visit(ASTConstructorDeclaration node, Object data) {
        return recurseOnExecutable(node);
    }


    public Object recurseOnExecutable(ASTMethodOrConstructorDeclaration node) {
        JExecutableSymbol sym = node.getSymbol();
        enclosingSymbols.push(sym);

        super.visit(node, null);

        enclosingSymbols.pop();
        return null;
    }

    public Object recurseOnClass(ASTAnyTypeDeclaration node) {
        InternalApiBridge.setQname(node, contextClassQName());

        JClassSymbol sym = symFactory.setClassSymbol(enclosingSymbols.getFirst(), node);
        enclosingSymbols.push(sym);

        super.visit(node, null);

        rollbackClassContext();
        enclosingSymbols.pop();
        return null;
    }


    private void updateContextForAnonymousClass() {
        updateClassContext("" + anonymousCounters.getFirst().incrementAndGet());
    }


    /**
     * Pushes a new context for an inner class.
     *
     * @param classInternalSimpleName Segment of the internal name (may contain the local index or be only numbers for
     *                                anon classes)
     */
    private void updateClassContext(String classInternalSimpleName) {
        String enclosing = innermostEnclosingTypeName.peek();
        if (enclosing != null) {
            innermostEnclosingTypeName.push(enclosing + "$" + classInternalSimpleName);
        } else {
            innermostEnclosingTypeName.push(packageName + "." + classInternalSimpleName);
        }
        anonymousCounters.push(new MutableInt(0));
        currentLocalIndices.push(new HashMap<>());
    }


    /** Rollback the context to the state of the enclosing class. */
    private void rollbackClassContext() {
        innermostEnclosingTypeName.pop();
        anonymousCounters.pop();
        currentLocalIndices.pop();
    }

    /** Creates a new class qname from the current context (fields). */
    private String contextClassQName() {
        return innermostEnclosingTypeName.getFirst();
    }


    /**
     * Gets the next available index based on a key and a histogram (map of keys to int counters).
     * If the key doesn't exist, we add a new entry with the startIndex.
     *
     * <p>Used for lambda and anonymous class counters
     *
     * @param histogram  The histogram map
     * @param key        The key to access
     * @param startIndex First index given out when the key doesn't exist
     *
     * @return The next free index
     */
    private static <T> int getNextIndexFromHistogram(Map<T, Integer> histogram, T key, int startIndex) {
        Integer count = histogram.get(key);
        if (count == null) {
            histogram.put(key, startIndex);
            return startIndex;
        } else {
            histogram.put(key, count + 1);
            return count + 1;
        }
    }

}
