# FastAPI Backend Bridge for InfoAnimal SR
# This bridges the native Android client with the PostgreSQL database in Render.
# Run with: uvicorn backend_bridge:app --host 0.0.0.0 --port 8000

import os
import uuid
import datetime
import jwt
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, HTTPException, Depends, Header, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr
import psycopg2
from psycopg2.extras import RealDictCursor

# FastAPI App Configuration
app = FastAPI(
    title="InfoAnimal SR API Bridge",
    description="REST API Backend bridging Android App with PostgreSQL on Render",
    version="1.0.0"
)

# CORS Policy
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# JWT Setup
JWT_SECRET = os.environ.get("JWT_SECRET", "super-secret-infoanimal-token-key-change-this")
JWT_ALGORITHM = "HS256"

# PostgreSQL Connection from Environment URL
DATABASE_URL = os.environ.get(
    "DB_EXTERNAL_URL",
    "postgresql://infoanimalsr_user:LK1pIHb5FmmEAAUrEhvzBXarIxcvLMi1@dpg-d8pvjlq8qa3s73ftn780-a.oregon-postgres.render.com/infoanimalsr"
)

def get_db_connection():
    """Helper to acquire database connection with DictCursor."""
    try:
        conn = psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)
        return conn
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Database connection error: {str(e)}"
        )

# --- Pydantic Request/Response schemas ---

class LoginRequest(BaseModel):
    identity: str  # Email
    password: str

class SignupRequest(BaseModel):
    email: EmailStr
    nombre: str
    telefono: Optional[str] = ""
    password: str

class UserProfileUpdate(BaseModel):
    nombre: Optional[str] = None
    telefono: Optional[str] = None
    zona_principal: Optional[str] = None
    foto_perfil: Optional[str] = None
    foto_portada: Optional[str] = None
    biografia: Optional[str] = None

class PostCreateRequest(BaseModel):
    tipo: str
    especie: str
    tamano: str
    nombre_mascota: Optional[str] = ""
    descripcion: str
    rasgos: Optional[str] = ""
    ubicacion_lat: Optional[float] = 0.0
    ubicacion_lng: Optional[float] = 0.0
    distrito: str
    zona: Optional[str] = ""
    telefono_contacto: Optional[str] = ""
    telefono_visible: Optional[bool] = True
    fotos: Optional[List[str]] = []

class CommentCreateRequest(BaseModel):
    post_id: str
    texto: str

class LikeCreateRequest(BaseModel):
    post_id: str

# Helper to verify token and extract user_id
def verify_token(authorization: Optional[str] = Header(None)) -> str:
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing authorization header")
    try:
        token = authorization.split(" ")[1] if " " in authorization else authorization
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        return payload["user_id"]
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")

# --- Endpoints ---

@app.get("/")
def read_root():
    return {"status": "healthy", "service": "InfoAnimal SR API Bridge", "database": "PostgreSQL"}

# 1. Users Auth: Login
@app.post("/api/users/login")
def login(req: LoginRequest):
    conn = get_db_connection()
    cur = conn.cursor()
    # Note: In production, passwords must be securely hashed! This is a base bridge template.
    cur.execute("SELECT * FROM users WHERE email = %s", (req.identity,))
    user = cur.fetchone()
    cur.close()
    conn.close()

    if not user:
        raise HTTPException(status_code=400, detail="Invalid credentials or user does not exist")
    
    if user["bloqueado"]:
        raise HTTPException(status_code=403, detail="User is blocked")

    # Generate token
    token = jwt.encode(
        {"user_id": user["id"], "exp": datetime.datetime.utcnow() + datetime.timedelta(days=30)},
        JWT_SECRET,
        algorithm=JWT_ALGORITHM
    )

    # Format created timestamp to string ISO format
    user_data = dict(user)
    if isinstance(user_data["created"], datetime.datetime):
        user_data["created"] = user_data["created"].isoformat()

    return {
        "token": token,
        "record": user_data
    }

# 2. Users Auth: Signup
@app.post("/api/users/signup", status_code=201)
def signup(req: SignupRequest):
    conn = get_db_connection()
    cur = conn.cursor()
    
    # Check if email already registered
    cur.execute("SELECT id FROM users WHERE email = %s", (req.email,))
    if cur.fetchone():
        cur.close()
        conn.close()
        raise HTTPException(status_code=400, detail="Email already registered")

    user_id = "usr_" + uuid.uuid4().hex[:8]
    
    # Simple role determination based on suffix
    rol = "user"
    if req.email.endswith("@infoanimals.com"):
        if "superadmin" in req.email:
            rol = "super_admin"
        elif "admin" in req.email:
            rol = "admin"
        elif "ong" in req.email:
            rol = "ong"

    cur.execute(
        """
        INSERT INTO users (id, email, nombre, telefono, rol, foto_perfil, foto_portada)
        VALUES (%s, %s, %s, %s, %s, %s, %s) RETURNING *
        """,
        (user_id, req.email, req.nombre, req.telefono, rol, "avatar_placeholder", "gradient_blue")
    )
    new_user = cur.fetchone()
    conn.commit()
    cur.close()
    conn.close()

    user_data = dict(new_user)
    if isinstance(user_data["created"], datetime.datetime):
        user_data["created"] = user_data["created"].isoformat()

    return user_data

# 3. Users Auth: Update Profile
@app.patch("/api/users/profile/{id}")
def update_profile(id: str, req: UserProfileUpdate, user_id: str = Depends(verify_token)):
    if id != user_id:
        raise HTTPException(status_code=403, detail="You can only modify your own profile")
        
    conn = get_db_connection()
    cur = conn.cursor()
    
    # Build dynamic update statement
    update_data = req.dict(exclude_unset=True)
    if not update_data:
        raise HTTPException(status_code=400, detail="No fields to update")
        
    set_clause = ", ".join([f"{k} = %s" for k in update_data.keys()])
    values = list(update_data.values())
    values.append(id)
    
    cur.execute(
        f"UPDATE users SET {set_clause} WHERE id = %s RETURNING *",
        values
    )
    updated_user = cur.fetchone()
    conn.commit()
    cur.close()
    conn.close()
    
    if not updated_user:
        raise HTTPException(status_code=404, detail="User not found")
        
    user_data = dict(updated_user)
    if isinstance(user_data["created"], datetime.datetime):
        user_data["created"] = user_data["created"].isoformat()
        
    return user_data

# 4. Posts: Get All
@app.get("/api/posts")
def get_posts(page: int = 1, perPage: int = 20, sort: str = "-created", filter: Optional[str] = None):
    conn = get_db_connection()
    cur = conn.cursor()
    
    # Construct base query
    query = """
        SELECT p.*, u.nombre as user_nombre_completo, u.foto_perfil as user_foto_perfil
        FROM posts p
        JOIN users u ON p.user_id = u.id
    """
    
    # Apply standard where filters if needed
    params = []
    if filter:
        # Simplistic parsing of filter parameter (e.g., "especie='perro'")
        if "=" in filter:
            field, val = filter.split("=")
            val = val.strip("'\"")
            query += f" WHERE p.{field} = %s"
            params.append(val)

    # Ordering
    order = "DESC" if sort.startswith("-") else "ASC"
    order_col = sort.lstrip("-")
    query += f" ORDER BY p.{order_col} {order}"
    
    # Pagination
    offset = (page - 1) * perPage
    query += " LIMIT %s OFFSET %s"
    params.extend([perPage, offset])
    
    cur.execute(query, params)
    posts_list = cur.fetchall()
    
    # Count total
    cur.execute("SELECT COUNT(*) as total FROM posts")
    total_items = cur.fetchone()["total"]
    
    cur.close()
    conn.close()
    
    # Transform timestamps
    formatted_posts = []
    for p in posts_list:
        post_dict = dict(p)
        if isinstance(post_dict["created"], datetime.datetime):
            post_dict["created"] = post_dict["created"].isoformat()
        # Mocking user expand block compatible with Android PocketBase parser expectations
        post_dict["user_id_expandido"] = post_dict["user_id"]
        formatted_posts.append(post_dict)
        
    total_pages = (total_items + perPage - 1) // perPage
    
    return {
        "page": page,
        "perPage": perPage,
        "totalItems": total_items,
        "totalPages": total_pages,
        "items": formatted_posts
    }

# 5. Posts: Create
@app.post("/api/posts", status_code=201)
def create_post(req: PostCreateRequest, user_id: str = Depends(verify_token)):
    conn = get_db_connection()
    cur = conn.cursor()
    
    post_id = "pst_" + uuid.uuid4().hex[:8]
    import json
    fotos_json = json.dumps(req.fotos)
    
    cur.execute(
        """
        INSERT INTO posts (id, user_id, tipo, especie, tamano, nombre_mascota, descripcion, rasgos,
                           ubicacion_lat, ubicacion_lng, distrito, zona, telefono_contacto, telefono_visible, fotos)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING *
        """,
        (post_id, user_id, req.tipo, req.especie, req.tamano, req.nombre_mascota, req.descripcion, req.rasgos,
         req.ubicacion_lat, req.ubicacion_lng, req.distrito, req.zona, req.telefono_contacto, req.telefono_visible, fotos_json)
    )
    new_post = cur.fetchone()
    conn.commit()
    cur.close()
    conn.close()
    
    post_data = dict(new_post)
    if isinstance(post_data["created"], datetime.datetime):
        post_data["created"] = post_data["created"].isoformat()
        
    return post_data

# 6. Posts: Delete
@app.delete("/api/posts/{id}", status_code=204)
def delete_post(id: str, user_id: str = Depends(verify_token)):
    conn = get_db_connection()
    cur = conn.cursor()
    
    # Retrieve first to confirm ownership
    cur.execute("SELECT user_id FROM posts WHERE id = %s", (id,))
    post = cur.fetchone()
    if not post:
        cur.close()
        conn.close()
        raise HTTPException(status_code=404, detail="Post not found")
        
    if post["user_id"] != user_id:
        # Check if user has admin privileges
        cur.execute("SELECT rol FROM users WHERE id = %s", (user_id,))
        role = cur.fetchone()["rol"]
        if role not in ["admin", "super_admin"]:
            cur.close()
            conn.close()
            raise HTTPException(status_code=403, detail="Unauthorized to delete this post")
            
    cur.execute("DELETE FROM posts WHERE id = %s", (id,))
    conn.commit()
    cur.close()
    conn.close()
    return None

# 7. Comments: Get Comments for Post
@app.get("/api/comments")
def get_comments(filter: Optional[str] = None):
    conn = get_db_connection()
    cur = conn.cursor()
    
    query = """
        SELECT c.*, u.nombre as user_nombre_completo, u.foto_perfil as user_foto_perfil
        FROM comments c
        JOIN users u ON c.user_id = u.id
    """
    params = []
    if filter:
        if "post_id=" in filter:
            val = filter.split("post_id=")[1].strip("'\"")
            query += " WHERE c.post_id = %s"
            params.append(val)
            
    query += " ORDER BY c.created ASC"
    cur.execute(query, params)
    comments_list = cur.fetchall()
    cur.close()
    conn.close()
    
    formatted_comments = []
    for c in comments_list:
        c_dict = dict(c)
        if isinstance(c_dict["created"], datetime.datetime):
            c_dict["created"] = c_dict["created"].isoformat()
        c_dict["user_id_expandido"] = c_dict["user_id"]
        formatted_comments.append(c_dict)
        
    return {
        "page": 1,
        "perPage": 100,
        "totalItems": len(formatted_comments),
        "totalPages": 1,
        "items": formatted_comments
    }

# 8. Comments: Create Comment
@app.post("/api/comments", status_code=201)
def create_comment(req: CommentCreateRequest, user_id: str = Depends(verify_token)):
    conn = get_db_connection()
    cur = conn.cursor()
    
    comment_id = "cmt_" + uuid.uuid4().hex[:8]
    cur.execute(
        "INSERT INTO comments (id, post_id, user_id, texto) VALUES (%s, %s, %s, %s) RETURNING *",
        (comment_id, req.post_id, user_id, req.texto)
    )
    new_comment = cur.fetchone()
    conn.commit()
    cur.close()
    conn.close()
    
    c_data = dict(new_comment)
    if isinstance(c_data["created"], datetime.datetime):
        c_data["created"] = c_data["created"].isoformat()
        
    return c_data

# 9. Veterinarias: Get All
@app.get("/api/veterinarias")
def get_veterinarias():
    conn = get_db_connection()
    cur = conn.cursor()
    cur.execute("SELECT * FROM veterinarias ORDER BY nombre ASC")
    vets_list = cur.fetchall()
    cur.close()
    conn.close()
    
    formatted_vets = []
    for v in vets_list:
        v_dict = dict(v)
        if isinstance(v_dict["created"], datetime.datetime):
            v_dict["created"] = v_dict["created"].isoformat()
        formatted_vets.append(v_dict)
        
    return {
        "page": 1,
        "perPage": 100,
        "totalItems": len(formatted_vets),
        "totalPages": 1,
        "items": formatted_vets
    }
