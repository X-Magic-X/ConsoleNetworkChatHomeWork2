CREATE TABLE public.users (
	user_id serial4 NOT NULL,
	username varchar(50) NOT NULL,
	"password" varchar(255) NOT NULL,
	created_at timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
	login varchar(50) NOT NULL,
	CONSTRAINT users_login_key UNIQUE (login),
	CONSTRAINT users_pkey PRIMARY KEY (user_id),
	CONSTRAINT users_username_key UNIQUE (username)
);
CREATE TABLE public.roles (
	role_id serial4 NOT NULL,
	role_name varchar(16) NOT NULL,
	CONSTRAINT roles_pkey PRIMARY KEY (role_id),
	CONSTRAINT roles_role_name_key UNIQUE (role_name)
);
CREATE TABLE public.user_roles (
	user_id int4 NOT NULL,
	role_id int4 NOT NULL,
	CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
	CONSTRAINT user_roles_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.roles(role_id) ON DELETE CASCADE,
	CONSTRAINT user_roles_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE
);
CREATE TABLE public.bans (
	ban_id serial4 NOT NULL,
	user_id int4 NULL,
	banned_by int4 NULL,
	reason text NULL,
	ban_start timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
	ban_end timestamptz NULL,
	CONSTRAINT bans_pkey PRIMARY KEY (ban_id),
	CONSTRAINT bans_banned_by_fkey FOREIGN KEY (banned_by) REFERENCES public.users(user_id) ON DELETE SET NULL,
	CONSTRAINT bans_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE
);