CREATE CONSTRAINT schema_entity_name_unique IF NOT EXISTS
FOR (entity:SchemaEntity)
REQUIRE entity.name IS UNIQUE;

WITH [
  {name: 'Profile', source: 'PostgreSQL.profile', key: 'id', description: 'Conta household, company, store ou admin; inclui status e restricoes alimentares.'},
  {name: 'Address', source: 'PostgreSQL.address', key: 'id', description: 'Endereco associado ao perfil, com latitude e longitude.'},
  {name: 'ProfilePhone', source: 'PostgreSQL.profile_phone', key: 'profile_id + phone', description: 'Telefones do perfil.'},
  {name: 'AuthMethod', source: 'PostgreSQL.auth_method', key: 'id', description: 'Credencial e provedor de autenticacao do perfil.'},
  {name: 'Company', source: 'PostgreSQL.company', key: 'id', description: 'Empresa associada a um perfil e a um plano.'},
  {name: 'Store', source: 'PostgreSQL.store', key: 'id', description: 'Loja da empresa, com perfil e horario de funcionamento.'},
  {name: 'Plan', source: 'PostgreSQL.plan', key: 'id', description: 'Plano comercial da empresa.'},
  {name: 'Payment', source: 'PostgreSQL.payment', key: 'id', description: 'Cobranca vinculada a empresa e plano.'},
  {name: 'Category', source: 'PostgreSQL.category', key: 'id', description: 'Categoria do catalogo de alimentos.'},
  {name: 'Food', source: 'PostgreSQL.food', key: 'id', description: 'Produto do catalogo; tambem representa o item alimentar referenciado por receitas.'},
  {name: 'PantryItem', source: 'PostgreSQL.pantry_item', key: 'id', description: 'Produto e quantidade existentes no estoque de um perfil.'},
  {name: 'PantryProductSetting', source: 'PostgreSQL.pantry_product_setting', key: 'id', description: 'Configuracao de quantidade minima de um produto no estoque.'},
  {name: 'Recipe', source: 'MongoDB.recipes', key: '_id', description: 'Receita salva e associada diretamente a um perfil por account_id.'},
  {name: 'ShoppingList', source: 'MongoDB.shopping_list', key: '_id', description: 'Lista de compras da casa, com data opcional.'},
  {name: 'Record', source: 'MongoDB.records', key: '_id', description: 'Atividade recente do perfil.'},
  {name: 'Notification', source: 'MongoDB.notifications', key: '_id', description: 'Notificacao direcionada a um perfil.'}
] AS entities
UNWIND entities AS entity
MERGE (node:SchemaEntity {name: entity.name})
SET node.source = entity.source,
    node.key = entity.key,
    node.description = entity.description;

WITH [
  {from: 'Profile', to: 'Address', name: 'LIVES_AT', source: 'profile.address_id -> address.id'},
  {from: 'Profile', to: 'ProfilePhone', name: 'HAS_PHONE', source: 'profile_phone.profile_id -> profile.id'},
  {from: 'Profile', to: 'AuthMethod', name: 'HAS_AUTH_METHOD', source: 'auth_method.profile_id -> profile.id'},
  {from: 'Company', to: 'Profile', name: 'HAS_PROFILE', source: 'company.profile_id -> profile.id'},
  {from: 'Company', to: 'Plan', name: 'SUBSCRIBES_TO', source: 'company.plan_id -> plan.id'},
  {from: 'Company', to: 'Store', name: 'HAS_STORE', source: 'store.company_id -> company.id'},
  {from: 'Company', to: 'Payment', name: 'HAS_PAYMENT', source: 'payment.company_id -> company.id'},
  {from: 'Payment', to: 'Plan', name: 'BILLED_FOR', source: 'payment.plan_id -> plan.id'},
  {from: 'Store', to: 'Profile', name: 'HAS_PROFILE', source: 'store.profile_id -> profile.id'},
  {from: 'Food', to: 'Category', name: 'IN_CATEGORY', source: 'food.category_id -> category.id'},
  {from: 'Profile', to: 'PantryItem', name: 'HAS_PANTRY_ITEM', source: 'pantry_item.profile_id -> profile.id'},
  {from: 'PantryItem', to: 'Food', name: 'OF_PRODUCT', source: 'pantry_item.food_id -> food.id'},
  {from: 'Profile', to: 'PantryProductSetting', name: 'HAS_PRODUCT_SETTING', source: 'pantry_product_setting.profile_id -> profile.id'},
  {from: 'PantryProductSetting', to: 'Food', name: 'FOR_PRODUCT', source: 'pantry_product_setting.food_id -> food.id'},
  {from: 'Profile', to: 'Recipe', name: 'SAVED', source: 'recipes.account_id -> profile.id'},
  {from: 'Recipe', to: 'Food', name: 'REQUIRES', source: 'recipes.ingredients[].food_id -> food.id'},
  {from: 'Profile', to: 'ShoppingList', name: 'HAS_SHOPPING_LIST', source: 'shopping_list.household_id -> profile.id'},
  {from: 'Profile', to: 'Record', name: 'HAS_RECORD', source: 'records.account_id -> profile.id'},
  {from: 'Profile', to: 'Notification', name: 'RECEIVES', source: 'notifications.profile_id -> profile.id'}
] AS relations
UNWIND relations AS relation
MATCH (source:SchemaEntity {name: relation.from})
MATCH (target:SchemaEntity {name: relation.to})
MERGE (source)-[edge:MODELS {name: relation.name, source: relation.source}]->(target);

MATCH path = (house:SchemaEntity {name: 'Profile'})-[:MODELS*1..5]-(entity:SchemaEntity)
RETURN path;

MATCH (house:Profile {id: $profileId, type: 'HOUSEHOLD'})
      -[:HAS_PANTRY_ITEM]->(:PantryItem)
      -[:OF_PRODUCT]->(inStock:Food)
      <-[:REQUIRES]-(recipe:Recipe)
      -[:REQUIRES]->(candidate:Food)
WHERE candidate.id <> inStock.id
  AND NOT EXISTS {
    MATCH (house)-[:HAS_PANTRY_ITEM]->(:PantryItem)-[:OF_PRODUCT]->(alreadyStocked:Food)
    WHERE alreadyStocked.id = candidate.id
  }
WITH candidate,
     count(DISTINCT recipe) AS recipesItPairsWith,
     collect(DISTINCT recipe.title)[0..5] AS exampleRecipes
RETURN candidate.id AS foodId,
       candidate.name AS product,
       recipesItPairsWith,
       exampleRecipes
ORDER BY recipesItPairsWith DESC, product ASC;
