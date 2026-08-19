from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database.db_dependency import get_db
from app.models.user_model import User
from app.models.alumni_model import Alumni
from app.schemas.user_schema import UserCreate, UserLogin, UserResponse, TokenResponse
from app.auth.auth_handler import hash_password, verify_password, create_access_token
from app.auth.jwt_dependency import get_current_user

router = APIRouter()

# Register User
@router.post("/register", status_code=status.HTTP_201_CREATED)
def register_user(user: UserCreate, db: Session = Depends(get_db)):
    existing_user = db.query(User).filter(User.email == user.email).first()

    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )

    hashed_pwd = hash_password(user.password)

    role = user.role.lower() if user.role else "student"
    if role == "admin":
        admin_exists = db.query(User).filter(User.role == "admin").first()
        if admin_exists:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Admin registration is restricted. Please contact system administrator."
            )

    new_user = User(
        name=user.name,
        email=user.email,
        password=hashed_pwd,
        role=role
    )

    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    # If registered as alumni, create default alumni directory record if not already present
    if new_user.role == "alumni":
        existing_alumni = db.query(Alumni).filter(Alumni.email == new_user.email).first()
        if not existing_alumni:
            new_alumni = Alumni(
                user_id=new_user.id,
                name=new_user.name,
                email=new_user.email
            )
            db.add(new_alumni)
            db.commit()

    token_data = {"sub": new_user.email, "role": new_user.role, "id": new_user.id}
    access_token = create_access_token(data=token_data)

    return {
        "message": "User registered successfully",
        "access_token": access_token,
        "token_type": "bearer",
        "user": {
            "id": new_user.id,
            "name": new_user.name,
            "email": new_user.email,
            "role": new_user.role
        }
    }

# Login User
@router.post("/login", response_model=TokenResponse)
def login_user(user: UserLogin, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.email == user.email).first()

    if not db_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid email or password"
        )

    if not verify_password(user.password, db_user.password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid email or password"
        )

    if hasattr(db_user, "is_active") and not db_user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Account has been deactivated"
        )

    token_data = {"sub": db_user.email, "role": db_user.role, "id": db_user.id}
    access_token = create_access_token(data=token_data)

    return {
        "message": "Login successful",
        "access_token": access_token,
        "token_type": "bearer",
        "user": {
            "id": db_user.id,
            "name": db_user.name,
            "email": db_user.email,
            "role": db_user.role
        }
    }

# Get Current Authenticated User Profile
@router.get("/me", response_model=UserResponse)
def get_current_user_profile(current_user: User = Depends(get_current_user)):
    return current_user