-- ====================================================================
-- RESERVA ROXOU - SCHEMA SUPABASE & POLÍCIAS DE SEGURANÇA (RLS)
-- ====================================================================
-- Este arquivo SQL deve ser executado no editor de SQL (SQL Editor) do 
-- painel do Supabase para configurar a base de dados online real 
-- do Reserva Roxou.
-- ====================================================================

-- Habilitar extensão UUID-OSSP para geração de UUID se necessário
create extension if not exists "uuid-ossp";

-- ==========================================
-- 1. TABELA DE PERFIS (PROFILES)
-- ==========================================
create table if not exists public.profiles (
    id uuid primary key,                     -- Chave primária associada ao auth.users(id) do Supabase
    name text not null,
    email text unique not null,
    avatar_url text,
    role text default 'passenger',           -- Papéis: 'passenger', 'driver', 'admin', 'passageiro', 'parceiro'
    created_at timestamptz default now(),
    
    constraint chk_role check (role in ('passenger', 'driver', 'admin', 'passageiro', 'parceiro'))
);

comment on table public.profiles is 'Perfis dos usuários do sistema Reserva Roxou.';

-- ==========================================
-- 2. TABELA DE STATUS DO MOTORISTA
-- ==========================================
create table if not exists public.driver_status (
    id uuid primary key default uuid_generate_v4(),
    driver_id uuid references public.profiles(id) on delete cascade unique, -- unique para permitir UPSERT on_conflict=driver_id
    status text default 'offline',           -- 'online', 'busy', 'offline'
    updated_at timestamptz default now(),
    
    constraint chk_status check (status in ('online', 'busy', 'offline'))
);

comment on table public.driver_status is 'Status operacional atual do motorista.';

-- ==========================================
-- 3. TABELA DE SOLICITAÇÕES DE RESERVA (RIDE REQUESTS)
-- ==========================================
create table if not exists public.ride_requests (
    id uuid primary key default uuid_generate_v4(),
    passenger_id uuid references public.profiles(id) on delete set null,
    origin text not null,
    destination text not null,
    distance_km numeric not null,
    trip_type text not null,                  -- 'one_way' (ida), 'round_trip' (ida_volta)
    passengers integer not null default 1,
    notes text,
    estimated_price numeric not null,
    final_price numeric,
    status text default 'pending',            -- 'pending', 'approved', 'rejected', 'confirmed', 'in_progress', 'completed', 'cancelled'
    payment_confirmed boolean default false,
    rejection_reason text,
    scheduled_at text,                        -- Tipo TEXT para prevenir erros de conversão de datas formatadas localmente no Brasil (ex: 30/05/2026 às 19:30)
    assigned_driver_id uuid references public.profiles(id) on delete set null,
    assigned_driver_name text,
    created_at timestamptz default now(),
    updated_at timestamptz default now(),
    
    constraint chk_trip_type check (trip_type in ('one_way', 'round_trip', 'ida', 'ida_volta')),
    constraint chk_ride_status check (status in ('pending', 'approved', 'rejected', 'confirmed', 'in_progress', 'completed', 'cancelled', 'pendente', 'aprovada', 'recusada', 'confirmada', 'em_viagem', 'concluída', 'cancelada')),
    constraint chk_passengers check (passengers >= 1 and passengers <= 4)
);

comment on table public.ride_requests is 'Solicitações de reservas e orçamentos de transporte de luxo.';

-- ==========================================
-- 4. TABELA DE MENSAGENS DO CHAT (RIDE MESSAGES)
-- ==========================================
create table if not exists public.ride_messages (
    id uuid primary key default uuid_generate_v4(),
    ride_id uuid references public.ride_requests(id) on delete cascade not null,
    sender_id uuid references public.profiles(id) on delete set null,
    message text not null,
    created_at timestamptz default now()
);

comment on table public.ride_messages is 'Canal de chat privado entre passageiro e motorista/admin por reserva.';

-- ==========================================
-- 5. TABELA ADICIONAL: LOCALIZAÇÃO AO VIVO DO MOTORISTA
-- ==========================================
create table if not exists public.driver_live_locations (
    driverId text not null,
    driverName text not null,
    latitude numeric not null,
    longitude numeric not null,
    timestamp bigint not null,
    requestId text primary key,               -- Chave primária baseada na reserva ativa para rastreamento único de viagem
    status text not null                      -- 'a_caminho', 'chegou', 'em_viagem'
);

comment on table public.driver_live_locations is 'Rastreamento por satélite em tempo real do veículo do motorista para o passageiro.';

-- ==========================================
-- 6. TRIGGER PARA ATUALIZAÇÃO AUTOMÁTICA DE UPDATED_AT
-- ==========================================
create or replace function public.handle_update_timestamp()
returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

create or replace trigger update_ride_requests_timestamp
    before update on public.ride_requests
    for each row
    execute function public.handle_update_timestamp();

-- ====================================================================
-- 7. FORÇAR AUTOMATICAMENTE CONTATO.FH3@GMAIL.COM COMO ADMIN (SEED AUTOMÁTICO)
-- ====================================================================
create or replace function public.force_admin_by_email()
returns trigger as $$
begin
    if lower(new.email) = 'contato.fh3@gmail.com' then
        new.role := 'admin';
    end if;
    return new;
end;
$$ language plpgsql;

create or replace trigger set_admin_role_on_profile
    before insert or update on public.profiles
    for each row
    execute function public.force_admin_by_email();

-- Se o perfil já existir no banco, força a atualização para admin
update public.profiles 
set role = 'admin' 
where lower(email) = 'contato.fh3@gmail.com';

-- ====================================================================
-- 8. POLÍCIAS DE SEGURANÇA (ROW LEVEL SECURITY - RLS)
-- ====================================================================

-- Ativar RLS para todas as tabelas
alter table public.profiles enable row level security;
alter table public.driver_status enable row level security;
alter table public.ride_requests enable row level security;
alter table public.ride_messages enable row level security;

-- ------------------------------------------
-- POLÍCIAS PARA: PROFILES
-- ------------------------------------------
create policy "Qualquer pessoa autenticada pode ver perfis"
    on public.profiles for select
    to authenticated
    using (true);

create policy "Usuários podem criar e atualizar seu próprio perfil"
    on public.profiles for all
    to authenticated
    using (auth.uid() = id)
    with check (auth.uid() = id);

-- ------------------------------------------
-- POLÍCIAS PARA: DRIVER_STATUS
-- ------------------------------------------
create policy "Qualquer pessoa autenticada pode ver o status do motorista"
    on public.driver_status for select
    to authenticated
    using (true);

create policy "Apenas motoristas/admins podem alterar seu próprio status"
    on public.driver_status for all
    to authenticated
    using (
        auth.uid() = driver_id 
        or exists (select 1 from public.profiles where id = auth.uid() and role = 'admin')
    )
    with check (
        auth.uid() = driver_id 
        or exists (select 1 from public.profiles where id = auth.uid() and role = 'admin')
    );

-- ------------------------------------------
-- POLÍCIAS PARA: RIDE_REQUESTS
-- ------------------------------------------
create policy "Passageiros veem apenas suas reservas; Admins e Motoristas veem tudo"
    on public.ride_requests for select
    to authenticated
    using (
        passenger_id = auth.uid()
        or exists (
            select 1 from public.profiles 
            where id = auth.uid() 
            and role in ('admin', 'driver', 'parceiro')
        )
    );

create policy "Passageiros autenticados podem solicitar novas reservas"
    on public.ride_requests for insert
    to authenticated
    with check (
        passenger_id = auth.uid()
        or exists (
            select 1 from public.profiles 
            where id = auth.uid() 
            and role in ('admin', 'driver', 'parceiro')
        )
    );

create policy "Apenas admins e motoristas podem alterar detalhes/status da reserva"
    on public.ride_requests for update
    to authenticated
    using (
        exists (
            select 1 from public.profiles 
            where id = auth.uid() 
            and role in ('admin', 'driver', 'parceiro')
        )
    )
    with check (
        exists (
            select 1 from public.profiles 
            where id = auth.uid() 
            and role in ('admin', 'driver', 'parceiro')
        )
    );

-- ------------------------------------------
-- POLÍCIAS PARA: RIDE_MESSAGES
-- ------------------------------------------
create policy "Passageiros e Motoristas veem mensagens de suas próprias reservas"
    on public.ride_messages for select
    to authenticated
    using (
        exists (
            select 1 from public.ride_requests r
            where r.id = ride_id
            and (
                r.passenger_id = auth.uid()
                or exists (
                    select 1 from public.profiles p
                    where p.id = auth.uid()
                    and p.role in ('admin', 'driver', 'parceiro')
                )
            )
        )
    );

create policy "Participantes da reserva autorizados podem enviar mensagens"
    on public.ride_messages for insert
    to authenticated
    with check (
        sender_id = auth.uid()
        and exists (
            select 1 from public.ride_requests r
            where r.id = ride_id
            and (
                r.passenger_id = auth.uid()
                or exists (
                    select 1 from public.profiles p
                    where p.id = auth.uid()
                    and p.role in ('admin', 'driver', 'parceiro')
                )
            )
        )
    );

-- ==========================================
-- 9. PREPARAR E ATIVAR REALTIME DO SUPABASE
-- ==========================================

-- Habilitar REPLICA IDENTITY FULL para capturar as mudanças completas de estados
alter table public.driver_status replica identity full;
alter table public.ride_requests replica identity full;
alter table public.ride_messages replica identity full;

-- Criar a publicação se ela ainda não existir
do $$
begin
  if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    create publication supabase_realtime;
  end if;
end;
$$;

-- Adicionar as tabelas na publicação do realtime canal online
alter publication supabase_realtime add table public.profiles;
alter publication supabase_realtime add table public.driver_status;
alter publication supabase_realtime add table public.ride_requests;
alter publication supabase_realtime add table public.ride_messages;
