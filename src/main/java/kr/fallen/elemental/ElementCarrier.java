package kr.fallen.elemental;
public interface ElementCarrier {
    Element elementalEyes$getElement();
    int elementalEyes$getElementTicks();
    void elementalEyes$setElement(Element e, int ticks);
    void elementalEyes$clearElement();
    String elementalEyes$getReaction();
    int elementalEyes$getReactionTicks();
    void elementalEyes$setReaction(String id, int ticks);
    void elementalEyes$markFireHit();
    boolean elementalEyes$fireHitActive();
}
