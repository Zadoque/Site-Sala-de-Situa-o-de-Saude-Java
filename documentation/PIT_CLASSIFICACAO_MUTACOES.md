# Classificação das mutações sobreviventes

Este documento registra a revisão do relatório PIT da branch
`refactor/modular-architecture-national-api`. O objetivo é
impedir que a porcentagem seja aumentada por testes artificiais.

## Resultado atual

- 215 mutações geradas;
- 187 mortas;
- mutation score: 87%;
- test strength: 93%;
- 13 mutações sem cobertura;
- 15 mutações sobreviventes.

Na execução por módulo, `analytics` teve 33 mutantes gerados, 27 mortos,
score de 82% e test strength de 96%; `user` teve 182 gerados, 160 mortos,
score de 88% e test strength de 92%.

Os gates de 80% para mutation score e test strength continuam ativos no Maven,
com `failWhenNoMutations=true`.

## Mutações sem cobertura

| Local | Classificação | Decisão |
|---|---|---|
| `EpidemiologyService` | Ramos sem cobertura em metadados, filtros e agregações | Manter a cobertura de resultados observáveis; revisar se o contrato analítico passar a expor esses ramos. |
| `AuthRateLimiter` | Ramos internos de eviction e limpeza | Manter testes de limite, expiração e concorrência; não expor estruturas internas apenas para matar mutantes. |
| `AuthenticationService` | Callbacks de conclusão de primeiro acesso e reset | Os fluxos públicos são cobertos por testes de contrato e integração; adicionar cenário se o contrato mudar. |
| `FirstAccessService` e `RefreshSessionService` | Callbacks de stream | Validar estado, hash, revogação e uso único; não criar testes artificiais de implementação. |
| `AuthenticationGateway` | Acessor de record sem cobertura isolada | O valor é exercitado pelos fluxos de autenticação; manter a verificação comportamental. |

Essas mutações permanecem visíveis no relatório e não são mascaradas por uma
exclusão ampla do PIT.

## Sobreviventes

| Local | Classificação | Decisão |
|---|---|---|
| `AdminUserService` | Defaults de entidade equivalentes nos fluxos de convite | Não criar teste artificial; preservar testes de estado observável. |
| `AuthRateLimiter` | Estados internos de janela/eviction sem diferença observável adicional | Manter os testes de limite, expiração e concorrência. |
| `FirstAccessService` e `RefreshSessionService` | Geração criptográfica aleatória | Validar formato, hash, revogação e uso único; não comparar amostras aleatórias. |
| `RefreshSessionService` | Validações já cobertas para tokens nulos, vazios e válidos | Manter a cobertura de contrato sem duplicar cenários equivalentes. |

Os sobreviventes acima não justificam reduzir o limiar, remover as asserções ou
adicionar testes que só observem implementação. Se o contrato de aleatoriedade,
limpeza de janelas ou datas padrão mudar, a classificação deve ser revisada.

## Regra para os próximos ciclos

Uma mutação sobrevivente só pode ser excluída após demonstrar que não existe
diferença observável válida ou que o caminho é estruturalmente inalcançável.
Caso contrário, o teste deve ser reescrito para verificar a regra de negócio,
ou o código deve ser corrigido quando a mutação revelar um defeito real.
