Compile time Annotations processor example

Annotation @Property for field, generates both getter and setter for annotated field
Features:
    - check if generated method is already exists
    - static and non-static fields support
    - if field is final, setter will not generated