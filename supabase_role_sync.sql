-- =========================================================================
-- Sincronización de Roles en el JWT de Supabase
-- =========================================================================
-- Este script crea una función de base de datos y un TRIGGER para asegurar que
-- cuando el campo 'rol' de un usuario sea actualizado en la tabla pública 'users',
-- dicho cambio se propague automáticamente a los metadatos nativos del usuario
-- ('raw_user_meta_data') en la tabla interna de autenticación de Supabase ('auth.users').
-- Esto permite que las reglas RLS basadas en el JWT contengan el rol actualizado en tiempo real.

-- 1. Función que actualiza los metadatos de autenticación nativos de Supabase
CREATE OR REPLACE FUNCTION public.sync_user_role_to_metadata()
RETURNS TRIGGER AS $$
BEGIN
    IF (OLD.rol IS DISTINCT FROM NEW.rol) THEN
        UPDATE auth.users
        SET raw_user_meta_data = 
            coalesce(raw_user_meta_data, '{}'::jsonb) || jsonb_build_object('rol', NEW.rol)
        WHERE id::text = NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Crear el Trigger asociado a nuestra tabla física de usuarios (public.users)
DROP TRIGGER IF EXISTS trigger_sync_user_role ON public.users;
CREATE TRIGGER trigger_sync_user_role
AFTER UPDATE ON public.users
FOR EACH ROW
EXECUTE FUNCTION public.sync_user_role_to_metadata();
